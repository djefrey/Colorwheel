<div align="center">
<img src=".github/logo.png" alt="Original logo by jnix, edited by djefrey" width="250">
<h1>Colorwheel</h1>
</div>
<br>

### About

The goal of this project is to provide a [Flywheel](https://github.com/Engine-Room/Flywheel) backend that is compatible with [Iris](https://github.com/IrisShaders/Iris) 1.x.

This project started as an attempt to port [Iris Flywheel Compat](https://github.com/leon-o/iris-flw-compat/) from Flywheel 0.6 to 1.0.  
However, with the amount of changes made to Flywheel, I decided to start from scratch.

Aperture, the new shader standard for Iris 2.0, is expected to support Flywheel.  
As such, this mod will no longer be required.  

### Difference with Iris Flywheel Compat

As of writing, Iris Flywheel Compat 1.x is able to adapt the shader code from the shaderpack to work with Flywheel 0.6.  
This was possible because the internal shaders used by Flywheel were very basic.

However, this changed drastically since Flywheel 1.0. The new release introduced many internal changes, including a new material system.  
Internal shaders are now much more complex (from 10 lines to over 1000 lines) and are generated on the fly, as mods such as Create use custom materials with custom shader code.

Colorwheel, instead, implements an extension to the Iris shader standard. This means shaderpacks must include specific programs that will be used by Colorwheel.

### How to use

To use this mod, you need to install Iris and a mod that includes Flywheel (like Create or Vanillin).  
You also need to use a compatible shaderpack.  

You can find Colorwheel releases and compatibility patches in the [Releases](https://github.com/djefrey/Colorwheel/releases) section.  
The [Colorwheel Patcher](https://github.com/djefrey/Colorwheel-Patcher) mod can be use to apply automatically patches on supported shaders.

### Credits

- **Jozufozu & PepperCode1** : Author and maintainers of Flywheel and Vanillin
- **leon-o** : Author of Iris Flywheel Compat
- **IMS** : Lead developer of Iris
- **douira** : Author of [glsl-transformer](https://github.com/IrisShaders/glsl-transformer)

### License

All code in this repository is licensed under **MIT**. You are free to read, distribute and modify the code.  
This does **not** apply to the shaderpack patches provided in the Releases section.

This project is partially based on code from Flywheel, licensed under MIT.  
