import os
from pathlib import Path
import shutil
import subprocess

__author__ = "carunodev"

base = Path(__file__).resolve().parent.parent
sdk = Path(os.environ.get("ANDROID_HOME", str(Path.home() / "AppData/Local/Android/Sdk")))
ndk = sdk / "ndk/26.3.11579264/toolchains/llvm/prebuilt/windows-x86_64/bin"
env = os.environ.copy()
env["CARGO_TARGET_AARCH64_LINUX_ANDROID_LINKER"] = str(ndk / "aarch64-linux-android29-clang.cmd")
env["CC_aarch64_linux_android"] = env["CARGO_TARGET_AARCH64_LINUX_ANDROID_LINKER"]
env["AR_aarch64_linux_android"] = str(ndk / "llvm-ar.exe")
env["CARGO_TARGET_ARMV7_LINUX_ANDROIDEABI_LINKER"] = str(ndk / "armv7a-linux-androideabi29-clang.cmd")
env["CC_armv7_linux_androideabi"] = env["CARGO_TARGET_ARMV7_LINUX_ANDROIDEABI_LINKER"]
env["AR_armv7_linux_androideabi"] = str(ndk / "llvm-ar.exe")
manifest = base / "audio-transport-rust/Cargo.toml"
subprocess.run(["cargo", "test", "--manifest-path", str(manifest)], env=env, check=True, timeout=600)

targets = [
    ("aarch64-linux-android", "arm64-v8a"),
    ("armv7-linux-androideabi", "armeabi-v7a"),
]
for rust_target, abi in targets:
    subprocess.run(["cargo", "build", "--locked", "--release", "--target", rust_target, "--manifest-path", str(manifest)], env=env, check=True, timeout=600)
    out = base / f"engine-android/src/main/jniLibs/{abi}/liborpheon_audio.so"
    out.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(base / f"audio-transport-rust/target/{rust_target}/release/liborpheon_audio.so", out)
    print(out)
