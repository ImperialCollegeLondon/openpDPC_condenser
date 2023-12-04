# -------------------------------------
# Phase reconstruction codes adapted from:
#
# https://github.com/Waller-Lab/DPC_withAberrationCorrection
# [1] L. Tian and L. Waller, Opt. Express 23, 11394-11403 (2015).
# [2] Z. F. Phillips, M. Chen, and L. Waller, PLOS ONE 12(2): e0171228 (2017).
# [3] M. Chen, Z. F. Phillips, and L. Waller, Opt. Express 26(25), 32888-32899 (2018).
# -------------------------------------


# %% import packages
import json
import math
from operator import is_
import os
import pathlib
import re
import socket
import sys
from ast import literal_eval
from dataclasses import field, asdict
from enum import IntEnum
from time import time
from typing import Iterable, Annotated

import cv2 as cv
import numpy as np
import tifffile as tiff
from PyQt5.QtCore import Qt
from PyQt5.QtWidgets import QScrollArea
from magicgui import widgets
from magicgui.experimental import guiclass
from magicgui.types import FileDialogMode
from scipy.ndimage import uniform_filter
from scipy.stats import mode

# %% useful constants
Q2S_ideal = (
    (1.0, 0.5, 0.5, 0.0),
    (0.5, 1.0, 0.0, 0.5),
    (0.5, 0.0, 1.0, 0.5),
    (0.0, 0.5, 0.5, 1.0),
)

Q2H = (
    (0.0, 0.0, 1.0, 1.0),
    (1.0, 0.0, 1.0, 0.0),
    (1.0, 1.0, 0.0, 0.0),
    (0.0, 1.0, 0.0, 1.0),
)

quadNorm_coeff_ideal = (1.0, 1.0, 1.0, 1.0)

seqs = [
    [0, 1, 2, 3],
    [0, 2, 1, 3],
    [1, 0, 3, 2],
    [2, 0, 3, 1],
    [3, 1, 2, 0],
    [3, 2, 1, 0],
    [1, 3, 0, 2],
    [2, 3, 0, 1],
]
num_seqs = len(seqs)  # 8
num_subs = len(seqs[0])  # 4


# %% useful utility functions & classes
class SP_pattern(IntEnum):
    """
    Enum class to determine which pattern to use in S, P, and img proc
    """

    quad = 0
    sub = 1
    half = 2


def F(img: np.ndarray) -> np.ndarray:
    return np.fft.fftshift(
        np.fft.fft2(np.fft.ifftshift(img, axes=(-2, -1)), axes=(-2, -1)), axes=(-2, -1)
    )


def IF(img: np.ndarray) -> np.ndarray:
    return np.fft.fftshift(
        np.fft.ifft2(np.fft.ifftshift(img, axes=(-2, -1)), axes=(-2, -1)), axes=(-2, -1)
    )


def get_S2Q(Q2S: Iterable[Iterable[float]] | None = None) -> Iterable[Iterable[float]]:
    output_type = type(Q2S) if Q2S is not None else tuple
    return output_type(
        map(output_type, np.linalg.pinv(np.asarray(Q2S_ideal if Q2S is None else Q2S)))
    )


def get_S2H(Q2S: Iterable[Iterable[float]] | None = None) -> Iterable[Iterable[float]]:
    output_type = type(Q2S) if Q2S is not None else tuple
    return output_type(map(output_type, np.asarray(Q2H) @ np.asarray(get_S2Q(Q2S))))


def load_image(path: str | os.PathLike | pathlib.Path):
    if path:
        path = os.path.expandvars(os.path.expanduser(path))
        if os.path.isfile(path):
            try:
                with tiff.TiffFile(path) as tif:
                    img = tif.asarray()
                print(f"Loaded image w. shape={img.shape:} dtype={img.dtype:}")
                return img
            except Exception as e:
                print(f"Failed to load image: {e:}")
                return None
        else:
            print(f"File not exist: {path:}")
            return None
    else:
        print(f"File not exist: {path:}")
        return None


def preproc_Bkg(Bkg_raw: np.ndarray | None) -> np.ndarray | None:
    if Bkg_raw is None:
        return None
    else:
        h, w = Bkg_raw.shape[-2:]
        return np.reshape(Bkg_raw, (-1, h, w)).mean(axis=0)


def binning_imgs(data: np.ndarray, binning: int = 1) -> np.ndarray:
    if binning > 1:
        h, w = tuple(binning * (sz // binning) for sz in data.shape[-2:])
        return np.stack(
            [
                data[..., :h, :w][
                    ..., index // binning :: binning, index % binning :: binning
                ]
                for index in range(binning**2)
            ],
            axis=0,
        ).mean(axis=0)
    else:
        return data


def add_magicgui_widget_to_vscrollarea(
    w: widgets, extent: list[int | None] | None = None
):
    if extent is None:
        extent = [None, None, None, None]

    scroll = QScrollArea()
    scroll.setWidget(w._widget._qwidget)

    scroll.setWidgetResizable(True)
    scroll.setSizeAdjustPolicy(QScrollArea.AdjustToContents)

    scroll.setHorizontalScrollBarPolicy(Qt.ScrollBarAlwaysOff)
    scroll.setVerticalScrollBarPolicy(Qt.ScrollBarAsNeeded)

    for i, sz in enumerate(extent):
        if sz is not None:
            match i:
                case 0:
                    scroll.setMinimumWidth(sz)
                case 1:
                    scroll.setMinimumHeight(sz)
                case 2:
                    scroll.setMaximumWidth(sz)
                case 3:
                    scroll.setMaximumHeight(sz)

    w._widget._qwidget = scroll


# %% read/write imagej tif
def read_imagej_tif(
    img_path: str | os.PathLike | pathlib.Path,
    tolerant_mode: bool = False,
) -> tuple[np.ndarray, tuple | None]:
    """
    Read imagej tif file

    Args:
        img_path: path to imagej tif file
        tolerant_mode: if True, will try to read data as np.ndarray even if
            imagej metadata is missing or inconsistent with data read in

    Returns:
        data (TZCYX(S)) as np.ndarray, (TZYX resolution value list, TZYX resolution unit list)

    """
    img_path = pathlib.Path(img_path)

    if img_path.exists() and img_path.suffix in [".tiff", ".tif", ".ome.tif"]:
        with tiff.TiffFile(img_path) as tif:
            # read data as np.ndarray
            try:
                data = tif.asarray()
            except:
                try:
                    data = np.stack([page.asarray() for page in tif.pages], axis=0)
                except:
                    raise IOError("Failed to read image data as np.ndarray")

            # read imagej metadata
            ij_tags = dict([(t.name, t.value) for t in tif.pages[0].tags])
            ij_mtdt = (
                tif.imagej_metadata
                if tif.imagej_metadata is not None
                else (
                    dict(re.findall(r"(.*)=(.*)\n", ij_tags["ImageDescription"]))
                    if "ImageDescription" in ij_tags.keys()
                    else None
                )
            )
            # print(ij_mtdt)
            # print(tif.pages[0].tags)

            if ij_tags and ij_mtdt is not None:
                # determine proper data shape
                data_shape_tzc = []
                for i, key in enumerate(["frames", "slices", "channels"]):
                    if key in ij_mtdt.keys():
                        data_shape_tzc.append(ij_mtdt[key])
                    else:
                        data_shape_tzc.append(1)
                # print(data_shape_tzc)

                # reshape data to the expected shape if needed
                Yax_ind = (
                    -3
                    if "SamplesPerPixel" in ij_tags.keys()
                    and ij_tags["SamplesPerPixel"] > 1
                    else -2
                )
                if data_shape_tzc != list(data.shape[:Yax_ind]):
                    # need reshape
                    if np.prod(data_shape_tzc) == np.prod(data.shape[:Yax_ind]):
                        new_shape = data_shape_tzc + list(data.shape[Yax_ind:])
                        data = np.reshape(data, newshape=new_shape, order="C")
                        print(
                            f"Reshape data w. newshape={data.shape:} to match expected TZC shape {data_shape_tzc:}."
                            + ("" if Yax_ind == -2 else " (S)")
                        )
                    else:
                        if tolerant_mode:
                            print(
                                f"Cannot reshape data w. shape={data.shape:} to match expected TZC shape {data_shape_tzc:}."
                                + ("" if Yax_ind == -2 else " (S)")
                            )
                            print("Will return image data as-is")
                        else:
                            raise ValueError(
                                f"Cannot reshape data w. shape={data.shape:} to match expected TZC shape {data_shape_tzc:}."
                                + ("" if Yax_ind == -2 else " (S)")
                            )

                # determine TZYX resolution
                res_per_unit = [
                    (ij_mtdt[key] if key in ij_mtdt.keys() else (0 if i == 0 else 1))
                    if i < 2
                    else (
                        (ij_tags.get(key)[1] / ij_tags.get(key)[0])
                        if key in ij_tags.keys()
                        else 1
                    )
                    for i, key in enumerate(
                        ["finterval", "spacing", "YResolution", "XResolution"]
                    )
                ]
                res_unit = [
                    ij_mtdt[key]
                    if key in ij_mtdt.keys()
                    else ("sec" if i == 0 else tiff.TIFF.RESUNIT.NONE)  # "-"
                    for i, key in enumerate(["tunit", "zunit", "yunit", "unit"])
                ]

                return data, (res_per_unit, res_unit)
            else:
                if tolerant_mode:
                    print("No metadata found - will just return image data as-is")
                    return data, None
                else:
                    raise ValueError("No metadata found")
    else:
        raise ValueError(f"Invalid image path: {img_path:}")


def write_imagej_tif(
    sv_path: str | os.PathLike | pathlib.Path,
    data: np.ndarray,
    res_info: tuple[list, list] | None = None,
    description: str = "",
):
    """
    Write imagej tif file

    Args:
        sv_path: path to save imagej tif file
        data: image data as np.ndarray, TZCYX(S)
        res_info: (TZYX resolution value list, TZYX resolution unit list)
        description: imagej description (show Info...)
    Returns:

    """
    sv_path = pathlib.Path(sv_path)
    if sv_path.suffix not in [".tiff", ".tif", ".ome.tif"]:
        raise ValueError("Invalid save path: {sv_path:}")
    if data.size <= 0 or data.ndim > 6 or data.ndim <= 0:
        raise ValueError(
            f"Cannot save array w. ndim={data.ndim:} (>6 or <=0) as ImageJ Hyperstack"
        )

    if not sv_path.parent.exists():
        print(f"Created directories: {sv_path:}")
        os.makedirs(os.path.dirname(sv_path))

    if data.ndim < 2:
        data = np.expand_dims(data, axis=np.arange(2 - data.ndim))
    if data.ndim == 6:
        if data.dtype not in [np.uint8, np.float32]:
            data = data.astype(np.float32)
    else:
        if data.dtype not in [np.uint8, np.uint16, np.float32]:
            data = data.astype(np.float32)

    ij_mtdt = {"Info": description + ("\n" if description else "")}
    res_keys = ["finterval", "spacing", "YResolution", "XResolution"]
    unit_keys = ["tunit", "zunit", "yunit", "unit"]
    if res_info is not None:
        for res_key, unit_key, res, res_unit in zip(
            res_keys, unit_keys, res_info[0], res_info[1]
        ):
            ij_mtdt[res_key] = res
            if res_unit is not tiff.TIFF.RESUNIT.NONE:
                ij_mtdt[unit_key] = res_unit

    with tiff.TiffWriter(sv_path, imagej=True) as tif:
        tif.write(
            data=data,
            metadata=ij_mtdt,
            resolution=tuple(1 / res for res in res_info[0][-2:][::-1])
            if res_info
            else None,
        )


# %% pDPC reconstruction related classes and functions


def preFT_SubBkg(imgs: np.ndarray, light_Bkg_imgs: np.ndarray | None) -> np.ndarray:
    """
    This function do bkg subtraction for images before doing FT and then used for deconvolve & phase reconstruction.
    If light_Bkg is None, get DC from light_Bkg; otherwise get from uniform filtering imgs

    Returns:
        imgs_subBkg -- ndarray w. shape=imgs.shape, (imgs/DC) - 1

    Args:
        imgs: array w. ndim>=3 & shape[0] = num_subs, num_sub-sp_patterned images to do FT & used for phase recon
        light_Bkg_imgs: 3d array w. (num_subs, h, w) | None, defualt None,
            light background for background subtraction.
    """
    expect_shape = imgs.shape
    h, w = imgs.shape[-2:]

    if light_Bkg_imgs is None:
        imgs_subBkg = np.reshape(
            np.stack(
                [
                    (img / uniform_filter(img, size=max(h, w) // 2)) - 1
                    for img in np.reshape(imgs, newshape=(-1, h, w))
                ],
                axis=0,
            ),
            newshape=expect_shape,
        )
    else:
        imgs_subBkg = np.reshape(
            (
                np.reshape(imgs, newshape=(num_subs, -1, h, w))
                / light_Bkg_imgs[:, None, ...]
            )
            - 1,
            newshape=expect_shape,
        )

    return imgs_subBkg


def deconv_Lsq_Phase(
    imgsF: np.ndarray,
    OTFs: tuple[np.ndarray, np.ndarray],
    reg_p: float = 1e-3,
    reg_u: float = 1e-5,
):
    """
    This function deconvolve img_display FT with TFs for phase reconstruction by analytical least square method

    :param imgsF: 2d YX FTs of images to be deconvolved
    :param OTFs: (H_mus, H_phis), 3d TFs of corresponding sp_pattern
    :param reg_p: regularization parameter for phase TF, default 1e-3
    :param reg_u: regularization parameter for amplitude TF, default 1e-3
    :return: phi, mu -- phase & amplitude img_display w. float32 type
    """
    t0 = time()
    expect_shape = imgsF.shape[1:]
    h, w = imgsF.shape[-2:]

    imgsF_4d = np.reshape(imgsF, newshape=(num_subs, -1, h, w))

    # adjust of TFs to match <imgsF> dimensions
    H_mus_4d = OTFs[0][:, None, ...]
    H_phis_4d = (1j * OTFs[1])[:, None, ...]

    # reconstruct phase by deconvolve & least-square analytical solution
    a1 = (H_phis_4d * H_phis_4d.conj()).sum(axis=0) + reg_p
    a2 = (H_mus_4d * H_mus_4d.conj()).sum(axis=0) + reg_u
    a3 = (H_mus_4d.conj() * imgsF_4d).sum(axis=0)
    a4 = (H_phis_4d.conj() * imgsF_4d).sum(axis=0)
    a5 = (H_mus_4d.conj() * H_phis_4d).sum(axis=0)
    a6 = (H_mus_4d * H_phis_4d.conj()).sum(axis=0)

    del H_mus_4d, H_phis_4d, imgsF_4d

    mu_4d = IF((a1 * a3 - a5 * a4) / (a1 * a2 - a6 * a5)).real
    phi_4d = IF((a2 * a4 - a6 * a3) / (a1 * a2 - a6 * a5)).real
    del a1, a2, a3, a4, a5, a6

    mu = np.reshape(mu_4d, newshape=tuple(expect_shape))
    phi = np.reshape(phi_4d, newshape=tuple(expect_shape))
    del mu_4d, phi_4d
    t1 = time()
    print(
        f"Done phase recon by Lsq deconv. w. phase shape={phi.shape:}, reg_p={reg_p:.3g}, reg_u={reg_u:.3g} "
        f"(Elapsed time: {t1 - t0:.3f} [sec])"
    )
    return phi, mu


def crop_OTFs(
    ary: np.ndarray, rawImgYXSize: tuple[int, int], binning: int = 1
) -> np.ndarray:
    """
    centerred-crop OTF size to match the target img_display size in the cases:
    1. when use_maxFlim=True in gen_freq()
    2. when binning > 1

    Args:
        ary: 3d array, amplitude TFs/phase TFs/S/P
        rawImgYXSize: tuple[int, int], (h, w) of raw polcam img_display, MUST be the same as input to gen_freq()
        binning: int, default 1, bin num

    Returns:
        cropped_H_mus, cropped_H_phis: (3d ndarray, 3d ndarray), cropped amplitude & phase TFs
    """
    expectOTFYXSize = tuple((sz // 2) // binning for sz in rawImgYXSize)
    startInds_YX = [
        szH // 2 - szExpect // 2
        for szH, szExpect in zip(ary.shape[-2:], expectOTFYXSize)
    ]

    return ary[
        ...,
        (startInds_YX[0] if ary.shape[-2] != 1 else 0) : (
            (startInds_YX[0] + expectOTFYXSize[0]) if ary.shape[-2] != 1 else 1
        ),
        (startInds_YX[1] if ary.shape[-1] != 1 else 0) : (
            (startInds_YX[1] + expectOTFYXSize[1]) if ary.shape[-1] != 1 else 1
        ),
    ]


def resize_OTFs(ary: np.ndarray, targetimgsF_YXsize: tuple[int, int]) -> np.ndarray:
    """
    resize OTF size to match the target img_display size in the cases:
    1. actual imgsF YX size is smaller/larger than the YXsize of OTFs calculated from rawYXsize given,
    but ZYX_px in image is the same.

    Args:
        ary: 3d array, amplitude TFs/phase TFs/S/P
        targetimgsF_YXsize: tuple[int, int], (h, w) of imgsF to be deconvolve w. OTFs

    Returns:
        resize_ary: 3d array, (h,w) same as targetimgsF_YXsize, shape at other dims same as ary
    """
    if targetimgsF_YXsize != ary.shape[-2:]:
        needreshape = ary.ndim > 3
        oldshape = list(ary.shape[:-2])
        if needreshape:
            ary = np.reshape(ary, (-1, ary.shape[-2], ary.shape[-1]))

        resize_ary = np.moveaxis(
            cv.resize(np.moveaxis(ary, 0, -1), dsize=targetimgsF_YXsize[::-1]), -1, 0
        )

        if needreshape:
            resize_ary = np.reshape(
                resize_ary, tuple(oldshape + list(resize_ary.shape[-2:]))
            )

        return resize_ary
    else:
        return ary


@guiclass
class pDPC_Params_GUI:
    # sysParams
    NA_illu: Annotated[float, {"min": 0, "step": 0.001}] = 0.6
    lbd_um: Annotated[float, {"min": 0, "step": 0.001}] = 0.632
    NA_img: Annotated[float, {"min": 0, "step": 0.001}] = 0.3
    mag: Annotated[float, {"min": 0, "step": 0.001}] = 10
    px_cam_um: Annotated[float, {"min": 0, "step": 0.001}] = 3.45
    use_pupil: bool = False
    Q2S: Annotated[
        tuple[
            tuple[float, float, float, float],
            tuple[float, float, float, float],
            tuple[float, float, float, float],
            tuple[float, float, float, float],
        ],
        {"layout": "vertical", "options": {"options": {"min": 0, "step": 0.001}}},
    ] = Q2S_ideal
    quadNorm_coeff: Annotated[
        tuple[float, float, float, float], {"options": {"min": 0, "step": 0.001}}
    ] = quadNorm_coeff_ideal
    quadGap_mm: Annotated[float, {"min": 0, "step": 0.001}] = 0.0
    quadRotate_rad: Annotated[float, {"min": 0, "step": 0.001}] = 0.0
    S_OffAxisYX_mm: Annotated[
        tuple[float, float], {"options": {"min": 0, "step": 0.001}}
    ] = (0.0, 0.0)
    P_OffAxisYX_mm: Annotated[
        tuple[float, float], {"options": {"min": 0, "step": 0.001}}
    ] = (0.0, 0.0)
    condenser_f_mm: Annotated[float, {"min": 0, "step": 0.001}] = field(
        default_factory=float
    )
    objective_f_mm: Annotated[float, {"min": 0, "step": 0.001}] = field(
        default_factory=float
    )

    # reconParams
    sp_pattern: SP_pattern = SP_pattern.half
    binning: Annotated[int, {"min": 1}] = 1
    seq: Annotated[int, {"min": 0, "max": num_seqs - 1}] = 0
    reg_p: Annotated[float, {"min": 1e-15, "step": 1e-5}] = 1.0e-3
    reg_u: Annotated[float, {"min": 1e-15, "step": 1e-5}] = 1.0e-5

    # bkgParams
    dark_bkg_path: Annotated[
        os.PathLike, dict(mode=FileDialogMode.EXISTING_FILE, filter="*.tif")
    ] = None
    light_bkg_path: Annotated[
        os.PathLike, dict(mode=FileDialogMode.EXISTING_FILE, filter="*.tif")
    ] = None

    def load_from_json_file(self, path: os.PathLike | pathlib.Path | str):
        if path is not None:
            path = os.path.expandvars(os.path.expanduser(path))
            if os.path.isfile(path) and os.path.basename(path).endswith(".json"):
                with open(path, mode="r") as f:
                    dict_params = json.load(f)
                try:
                    for k, v in dict_params.items():
                        getattr(self.gui, k).value = v
                except Exception as e:
                    print("Failed to update params from json file to GUI")
                    # print(e)
            else:
                print(f"Failed to load from an invalid path: {path:}")
        else:
            print(f"Failed to load from an invalid path: {path:}")

    def get_json(self):
        thisdict = asdict(self)
        try:
            jstr = json.dumps(thisdict, indent=4)
            return jstr
        except:
            thisdict["dark_bkg_path"] = str(thisdict["dark_bkg_path"])
            thisdict["light_bkg_path"] = str(thisdict["light_bkg_path"])
            try:
                jstr = json.dumps(thisdict, indent=4)
                return jstr
            except:
                print("Failed to convert to json")
                return ""

    def export_to_json_file(self, path: os.PathLike | pathlib.Path | str):
        if path is not None:
            path = os.path.expandvars(os.path.expanduser(path))
            if os.path.isdir(os.path.dirname(path)) and os.path.basename(path).endswith(
                ".json"
            ):
                with open(path, mode="w") as f:
                    json.dump(asdict(self), f)
                print(f"Exported params to: {path:}")
            else:
                print(f"Failed to export to an invalid path: {path:}")
        else:
            print(f"Failed to export to an invalid path: {path:}")

    def __inner_updateParameters(self):
        """
        This is the private function to update public attributes.
        Should be called if any system parameters have been changed
        """
        self.S2Q: Iterable[Iterable[float]] = get_S2Q(self.Q2S)
        self.S2H: Iterable[Iterable[float]] = get_S2H(self.Q2S)

        self.px_um: float = self.px_cam_um * 2 / self.mag
        self.imgFlim: float = self.NA_img / self.lbd_um
        self.illuFlim: float = self.NA_illu / self.lbd_um
        self.camFlim: float = 1 / (2 * self.px_um)

        self.S_moveYX_F: tuple = tuple(
            (
                vi / (self.condenser_f_mm * self.lbd_um)
                if vi != 0 and self.condenser_f_mm != 0
                else 0
            )
            for vi in self.S_OffAxisYX_mm
        )
        self.P_moveYX_F: tuple = tuple(
            (
                vi / (self.objective_f_mm * self.lbd_um)
                if vi != 0 and self.objective_f_mm != 0
                else 0
            )
            for vi in self.P_OffAxisYX_mm
        )
        self.quadGap_F: float = (
            (
                (abs(self.quadGap_mm) / 2)
                / (
                    (self.objective_f_mm if self.use_pupil else self.condenser_f_mm)
                    * self.lbd_um
                )
            )
            if (self.quadGap_mm != 0)
            and (
                (self.use_pupil and self.objective_f_mm != 0)
                or ((not self.use_pupil) and self.condenser_f_mm != 0)
            )
            else 0
        )

        self.maxilluFlim: float = self.illuFlim + max(
            [abs(vi) for vi in self.S_moveYX_F]
        )
        self.maximgFlim: float = self.imgFlim + max([abs(vi) for vi in self.P_moveYX_F])

    def __inner_update_light_bkg(self):
        self.light_bkg_4 = self.gen_imgs(raw=self.light_bkg)
        print(
            "Updated light_bkg_4: "
            + (
                f"shape={self.light_bkg_4.shape:}"
                if self.light_bkg_4 is not None
                else "None"
            )
        )

    def __post_init__(self):
        self.dark_bkg: np.ndarray | None = None
        self.light_bkg: np.ndarray | None = None
        self.__inner_updateParameters()
        self.__inner_update_light_bkg()

        self.isOTFsReady: bool = False
        self.isImgsReady: bool = False

        self.events.connect(self.on_any_change)

        self.gui.dark_bkg_path.value = os.path.expanduser("~")
        self.gui.light_bkg_path.value = os.path.expanduser("~")

        self.gui.dark_bkg_path.changed.connect(self.getFunc_load_bkg(name="dark"))
        self.gui.light_bkg_path.changed.connect(self.getFunc_load_bkg(name="light"))

        # somehow self.events.connect cannot detect value change in TupleEdit GUI
        # connect their change events separately as below
        keys = ["Q2S", "quadNorm_coeff", "S_OffAxisYX_mm", "P_OffAxisYX_mm"]
        for k in keys:
            getattr(self.gui, k).changed.connect(self.getFunc_manual_link(label=k))

        # make it scrollable
        add_magicgui_widget_to_vscrollarea(self.gui, extent=[650, None, None, None])

    def getFunc_load_bkg(self, name: str):
        def load_bkg(value: os.PathLike):
            setattr(self, name + "_bkg", preproc_Bkg(load_image(value)))
            bkg = getattr(self, name + "_bkg")
            setattr(self, name + "_bkg_path", None if bkg is None else value.__str__())
            setattr(
                getattr(self.gui, name + "_bkg_path"),
                "label",
                name
                + " bkg path"
                + (f": {tuple(bkg.shape):}" if bkg is not None else ""),
            )

            if bkg is not None:
                print(f"Loaded {name:} bkg w. shape={bkg.shape:}")
            else:
                print(f"Failed to load {name:} bkg from {value:}")

        return load_bkg

    def getFunc_manual_link(self, label: str):
        def manual_link(value):
            setattr(self, label, value)

        return manual_link

    def on_any_change(self, info):
        # print changes -- for debug only
        print(f"field {info.signal.name!r} changed to {info.args}")

        # update flags & attr separately
        if info.signal.name not in [
            "seq",
            "dark_bkg_path",
            "light_bkg_path",
            "reg_p",
            "reg_u",
        ]:
            if info.signal.name not in ["sp_pattern", "binning"]:
                # inner update all sys-parameters needed
                self.__inner_updateParameters()
            self.isOTFsReady = False

        if info.signal.name in [
            "Q2S",
            "sp_pattern",
            "binning",
            "seq",
            "dark_bkg_path",
            "light_bkg_path",
        ]:
            self.isImgsReady = False
            self.__inner_update_light_bkg()

    def get_sub_imgs(self, raw: np.ndarray | None) -> np.ndarray | None:
        """
        This function takes raw polcam img_display(s) and returns sub images stacked in the first axis with an order
        determined by (seq mod 8).
        While generating sub images, raw img_display(s) YX sizes will be cropped down to
        closest even values so that all sub images have the same YX sizes.
        Input arrays should comply with self.use_gpu

        :param raw: array w. ndim>=2, raw polarisation camera images
        :return: sub_imgs -- sub-images stacked at first axis
        """
        if raw is None:
            return None
        else:
            # determine sequence of sub-images determined by seq
            sequence = [seqs[self.seq % num_seqs].index(i) for i in range(num_subs)]

            # get sub-images by slicing array using index & in the right sequence order
            h, w = tuple(2 * (sz // 2) for sz in raw.shape[-2:])
            raw = raw[..., :h, :w]

            if self.dark_bkg is not None:
                raw = raw - self.dark_bkg[:h, :w]
                raw *= raw >= 0

            sub_imgs = binning_imgs(
                np.stack(
                    [raw[..., index // 2 :: 2, index % 2 :: 2] for index in sequence],
                    axis=0,
                ),
                binning=self.binning,
            )

            return sub_imgs

    def gen_imgs(self, raw: np.ndarray | None) -> np.ndarray | None:
        """
        Generate sub/quad/half images of raw.
        Get sub-images first and then transform to other patterns if needed.

        Args:
            raw: ndarray w. ndim>=2

        Returns:
            ndarray, images ready to be used for background subtraction & FT
        """
        if raw is None:
            return None
        else:
            imgs: np.ndarray = self.get_sub_imgs(raw)

            if self.sp_pattern is not SP_pattern.sub:
                mtx = np.asarray(
                    self.S2H if self.sp_pattern is SP_pattern.half else self.S2Q
                )
                imgs = np.tensordot(mtx, imgs, axes=(-1, 0))

            return imgs

    def gen_freq(
        self, rawImgYXSize: tuple[int, int], use_maxFlim: bool = True
    ) -> tuple[np.ndarray, np.ndarray]:
        """
        generate spatial frequencies w. origin at img_display center for future use in generate S, P, OTFs.
        If use_maxFlim, the length of fy, fx will be extended to include maximum frequency limit in the system.

        Args:
            rawImgYXSize: tuple[int, int]
            use_maxFlim: bool, default True

        Returns:
            fy, fx -- (2d array w. shape[-1]=1, 2d array w. shape[-1]=1), spatial frequencies used to calculate S, P, OTFs
        """
        if use_maxFlim:
            Flim = max(self.camFlim, self.maxilluFlim, self.maximgFlim)
        else:
            Flim = self.camFlim

        df_YX = [1 / (self.px_um * (sz // 2)) for sz in rawImgYXSize]
        actual_freq_YXsz = [
            2 * math.ceil((Flim - self.camFlim) / df) + (sz // 2)
            for sz, df in zip(rawImgYXSize, df_YX)
        ]
        fy, fx = tuple(
            (np.arange(sz) - sz // 2) * df for sz, df in zip(actual_freq_YXsz, df_YX)
        )
        fy = fy[:, None]
        fx = fx[None, :]

        return fy, fx

    def gen_S_P(
        self, fy: np.ndarray, fx: np.ndarray, abundant_output: bool = False
    ) -> tuple:
        """
        Generate source and pupil function corr. to sp_pattern under corrd. by fy, fx
        First generate S, P under quad pattern and then transform to other patterns if needed.

        Args:
            fy: 2d array
            fx: 2d array

        Returns:
            S, P -- tuple(3d array, 3d array), source and pupil function
        """
        if abundant_output:
            S, P, A_outputs = self.gen_quad_S_P(fy, fx, abundant_output)
        else:
            S, P = self.gen_quad_S_P(fy, fx, abundant_output)

        if self.sp_pattern == SP_pattern.quad:
            if abundant_output:
                return S, P, A_outputs
            else:
                return S, P
        else:
            mtx = np.asarray(self.Q2S if self.sp_pattern == SP_pattern.sub else Q2H)
            if self.use_pupil:
                P = np.tensordot(
                    mtx**0.5, P, axes=(-1, 0)
                )  # pupil function is w.r.t. amplitude rather than intensity
                # P = np.einsum( "ij,j...->i...", mtx, P)
            else:
                S = np.tensordot(mtx, S, axes=(-1, 0))
                # S = np.einsum("ij,j...->i...", mtx, S)
            if abundant_output:
                return S, P, A_outputs
            else:
                return S, P

    def gen_quad_S_P(
        self, fy: np.ndarray, fx: np.ndarray, abundant_output: bool = False
    ) -> tuple:
        """
        Generate source & pupil function corr. to quad pattern w. coord. by fy, fx

        Args:
            fy: 2d ndarray
            fx: 2d ndarray

        Returns:
            S, P: tuple(3d array, 3d array), source and pupil function

        """
        angles = [0, math.pi / 2, 3 * math.pi / 2, math.pi]
        tolerance = 1e-15

        if abundant_output:
            S_ideal = (fy**2 + fx**2 + tolerance <= (self.illuFlim**2))[None, ...]
            P_ideal = (fy**2 + fx**2 + tolerance <= (self.imgFlim**2))[None, ...]

        S = (
            (fy - self.S_moveYX_F[0]) ** 2 + (fx - self.S_moveYX_F[1]) ** 2 + tolerance
            <= (self.illuFlim**2)
        )[None, ...]
        P = (
            (
                ((fy - self.P_moveYX_F[0]) ** 2)
                + ((fx - self.P_moveYX_F[1]) ** 2)
                + tolerance
            )
            <= (self.imgFlim**2)
        )[None, ...]

        moveYX_F = self.P_moveYX_F if self.use_pupil else self.S_moveYX_F
        masks = np.stack(
            [
                coeff
                * (
                    math.cos(angle + math.pi + self.quadRotate_rad) * (fy - moveYX_F[0])
                    > math.sin(angle + math.pi + self.quadRotate_rad)
                    * (fx - moveYX_F[1])
                    + tolerance
                    + self.quadGap_F
                )
                * (
                    math.cos(angle + math.pi * 3 / 2 + self.quadRotate_rad)
                    * (fy - moveYX_F[0])
                    + tolerance
                    + self.quadGap_F
                    < math.sin(angle + math.pi * 3 / 2 + self.quadRotate_rad)
                    * (fx - moveYX_F[1])
                )
                for coeff, angle in zip(self.quadNorm_coeff, angles)
            ],
            axis=0,
        )

        if self.use_pupil:
            P = P * masks
        else:
            S = S * masks

        if abundant_output:
            S_allquads = np.sum(S, axis=0, keepdims=True)
            P_allquads = np.sum(P, axis=0, keepdims=True)

        if abundant_output:
            return S, P, (S_allquads, P_allquads, S_ideal, P_ideal)
        else:
            return S, P

    @staticmethod
    def gen_OTFs(
        S: np.ndarray, P: np.ndarray
    ) -> tuple[np.ndarray, np.ndarray, np.ndarray]:
        """
        generate optical transfer function w. given source and pupil function

        Args:
            S: 3d array, source function
            P: 3d array, pupil function

        Returns:
            H_mus, H_phis, Bkgs: (3d array, 3d array, 3d array), amplitude TFs, phase TFs, Bkg values of each TF
        """

        # compute OTF_tools by TCC
        Bkgs = np.sum((S * (P * P.conj())).real, axis=(-2, -1), keepdims=True)
        F1 = F(S.conj() * P)
        G1 = F(P)
        TCC = IF(F1.conj() * G1)
        TCC_inv_conj = IF(G1.conj() * F1)

        del F1, G1

        H_mus = ((TCC + TCC_inv_conj) / Bkgs).real
        H_phis = ((TCC - TCC_inv_conj) / Bkgs).real

        del TCC, TCC_inv_conj

        return H_mus, H_phis, Bkgs

    def gen_OTFs_shortcut(
        self,
        rawImgYXSize: tuple[int, int] = (2448, 2048),
        abundant_output: bool = False,
    ):
        t0 = time()
        fy, fx = self.gen_freq(rawImgYXSize=rawImgYXSize, use_maxFlim=True)

        if abundant_output:
            S, P, A_outputs = self.gen_S_P(
                fy=fy, fx=fx, abundant_output=abundant_output
            )
        else:
            S, P = self.gen_S_P(fy=fy, fx=fx, abundant_output=abundant_output)

        Hmus, Hphis, Bkgs = self.gen_OTFs(S=S, P=P)
        Hmus, Hphis, S, P, Bkgs, fy, fx = tuple(
            crop_OTFs(ary=ary, rawImgYXSize=rawImgYXSize, binning=self.binning)
            for ary in (Hmus, Hphis, S, P, Bkgs, fy, fx)
        )

        if abundant_output:
            S_allquads, P_allquads, S_ideal, P_ideal = A_outputs

            Hmus_allquads, Hphis_allquads, Bkgs_allquads = self.gen_OTFs(
                S_allquads, P_allquads
            )
            Hmus_ideal, Hphis_ideal, Bkgs_ideal = self.gen_OTFs(S_ideal, P_ideal)

            (
                Hmus_allquads,
                Hphis_allquads,
                S_allquads,
                P_allquads,
                Bkgs_allquads,
                Hmus_ideal,
                Hphis_ideal,
                S_ideal,
                P_ideal,
                Bkgs_ideal,
            ) = tuple(
                crop_OTFs(ary=ary, rawImgYXSize=rawImgYXSize, binning=self.binning)
                for ary in (
                    Hmus_allquads,
                    Hphis_allquads,
                    S_allquads,
                    P_allquads,
                    Bkgs_allquads,
                    Hmus_ideal,
                    Hphis_ideal,
                    S_ideal,
                    P_ideal,
                    Bkgs_ideal,
                )
            )

        t1 = time()
        print(
            f"Generated OTFs for rawImgYXSize={rawImgYXSize:}, "
            + f"sp_pattern={self.sp_pattern:}, binning={self.binning:}. "
            + f"(Elapsed time: {t1 - t0:0.3f} [sec])"
        )

        if abundant_output:
            return (
                Hmus,
                Hphis,
                S,
                P,
                Bkgs,
                fy,
                fx,
                (
                    Hmus_allquads,
                    Hphis_allquads,
                    S_allquads,
                    P_allquads,
                    Bkgs_allquads,
                    Hmus_ideal,
                    Hphis_ideal,
                    S_ideal,
                    P_ideal,
                    Bkgs_ideal,
                ),
            )
        else:
            return Hmus, Hphis, S, P, Bkgs, fy, fx

    def prepare_raw_shortcut(self, raw: np.ndarray):
        t0 = time()
        raw_4 = self.gen_imgs(raw=raw)
        raw_4_F = F(preFT_SubBkg(raw_4, self.light_bkg_4))
        t1 = time()
        print(
            f"Prepared raw w. shape={raw.shape:}. "
            + f"(Elapsed time: {t1 - t0:0.3f} [sec])"
        )
        return raw_4_F, raw_4


class pDPC_MM2(pDPC_Params_GUI):
    def __init__(self, **kwargs):
        super().__init__(**kwargs)

        self.isOTFsReady: bool = False
        self.OTFs: tuple | None = None

    def on_any_change(self, info):
        super().on_any_change(info)
        if info.signal.name not in [
            "seq",
            "reg_p",
            "reg_u",
            "dark_bkg_path",
            "light_bkg_path",
        ]:
            self.isOTFsReady = False

    def recon(self, raw):
        raw_4_F, _ = self.prepare_raw_shortcut(raw=raw)
        if (
            self.OTFs is None
            or not self.isOTFsReady
            or raw_4_F.shape[-2:] != self.OTFs[0].shape[-2:]
        ):
            self.OTFs = self.gen_OTFs_shortcut(rawImgYXSize=raw.shape[-2:])[:2]
            self.isOTFsReady = True

        phase, _ = deconv_Lsq_Phase(
            imgsF=raw_4_F,
            OTFs=self.OTFs,
            reg_p=self.reg_p,
            reg_u=self.reg_u,
        )

        return phase.astype(np.float32)


if __name__ == "__main__":
    ideal_pDPC_params = {
        "Q2S": Q2S_ideal,
        "quadNorm_coeff": quadNorm_coeff_ideal,
        "quadGap_mm": 0.0,
        "quadRotate_rad": 0.0,
        "S_OffAxisYX_mm": (0.0, 0.0),
        "P_OffAxisYX_mm": (0.0, 0.0),
        "condenser_f_mm": 30,
        "objective_f_mm": 18,
        "sp_pattern": SP_pattern.sub,
        "dark_bkg_path": None,
        "light_bkg_path": None,
    }
    pDPC_ut = pDPC_MM2(**ideal_pDPC_params)

    try:
        port = int(sys.argv[1])
        goodsign = sys.argv[2]
        badsign = sys.argv[3]
        exitsign = sys.argv[4]
    except Exception as e:
        print(e)
        port = -1

    if port > 0:
        host = "localhost"
        s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        print("python program started")
        a = input("proceed? ")
        print(a)

        socket_connected = False
        try:
            s.connect((host, port))
            socket_connected = True
        except Exception as e:
            print(e)
            print("Failed to connect to server")

        if socket_connected:
            print("Listening from:" + str(port) + " on " + str(host))
            while True:
                try:
                    data = s.recv(1024).decode("utf-8").strip()
                except:
                    print("Failed to receive data")
                    break

                if exitsign in data:
                    break
                else:
                    print(data)
                    info = goodsign
                    try:
                        parse_items = re.findall(
                            r"((live:)|(recon:))rawPath:(.*); phasePath:(.*); Params:(.*); useAdvParams:(.*)",
                            data,
                        )
                        if parse_items:
                            live_or_recon = parse_items[0][0].lower()
                            rawpath = parse_items[0][3]
                            phasepath = parse_items[0][4]
                            params = json.loads(parse_items[0][5])
                            useAdvParams = parse_items[0][6].lower()
                        else:
                            raise ValueError("Failed to parse data")

                        # set live or recon
                        is_live = True
                        if live_or_recon.startswith("l"):
                            is_live = True
                        elif live_or_recon.startswith("r"):
                            is_live = False
                        else:
                            raise ValueError("Failed to parse data: " + live_or_recon)

                        # set params for pDPC_ut
                        use_advparam = True
                        if useAdvParams.startswith("t"):
                            use_advparam = True
                        elif useAdvParams.startswith("f"):
                            use_advparam = False
                        else:
                            raise ValueError("Failed to parse data: " + useAdvParams)

                        expect_keys = set(asdict(pDPC_ut).keys())
                        if not use_advparam:
                            expect_keys = expect_keys - set(ideal_pDPC_params.keys())
                            print(
                                "Using default (ideal) params for: "
                                + f"{ideal_pDPC_params.keys()}"
                            )

                        received_keys = set(params.keys())
                        assert expect_keys.issubset(received_keys), (
                            "Params not match. "
                            + f"Missing params: {[k for k in expect_keys if k not in received_keys]}"
                        )

                        try:
                            know = ""
                            for k in list(expect_keys):
                                know = k
                                vnow = params[k]
                                if vnow is None and k not in [
                                    "dark_bkg_path",
                                    "light_bkg_path",
                                ]:
                                    raise Exception(f"Param {k} MUST not be None")
                                else:
                                    getattr(pDPC_ut.gui, k).value = (
                                        vnow
                                        if not isinstance(vnow, str)
                                        else literal_eval(vnow)
                                    )
                        except Exception as e:
                            raise Exception(f"Failed to set params {know}: {e:}")

                        if not is_live:
                            info = goodsign + ":0"
                            s.sendall((info + "\r\n").encode("utf-8"))

                        # pDPC recon
                        raw, _ = read_imagej_tif(img_path=rawpath, tolerant_mode=True)

                        h, w = raw.shape[-2:]
                        oldshape = raw.shape[:-2]
                        raw_3d = np.reshape(raw, newshape=(-1, h, w))

                        if is_live:
                            phase_3d = pDPC_ut.recon(raw=raw_3d)
                        else:
                            phase_3d = []
                            for i in range(raw_3d.shape[0]):
                                phase_3d.append(pDPC_ut.recon(raw=raw_3d[i]))
                                info = (
                                    goodsign
                                    + f":{max(0, ((100*(i+1))//raw_3d.shape[0])-1):d}"
                                )
                                s.sendall((info + "\r\n").encode("utf-8"))
                            info = goodsign + f":100"
                            phase_3d = np.stack(phase_3d, axis=0)

                        phase = np.reshape(
                            phase_3d, newshape=oldshape + phase_3d.shape[-2:]
                        )

                        if is_live:
                            # subtract phase image mode before saving
                            hist, bin_edges = np.histogram(phase[:], bins=256)
                            idx = np.argmax(hist)
                            modevalue = (bin_edges[idx] + bin_edges[idx + 1]) / 2
                            print(modevalue)
                            # modevalue = mode(phase[:] ,keepdims=False)[0]
                            phase = phase - modevalue

                        write_imagej_tif(
                            sv_path=phasepath,
                            data=phase,
                            res_info=None,
                            description=pDPC_ut.get_json(),
                        )

                    except Exception as e:
                        info = badsign + f":{e}"

                    s.sendall((info + "\r\n").encode("utf-8"))
        s.close()
        print("Socket closed")
    else:
        print(f"Invalid port number: {port}")
