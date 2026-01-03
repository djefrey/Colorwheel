### 1.2.0

- Added compatibility with Flywheel 1.0.6 (used by Create 6.0.9)
- Fixed bug causing very large bright flashes on Euphoria Patches
- Fixed `mc_midTexCoord` on Create large waterwheels

### 1.1.2

- Fixed crash when loading a PBR resource pack
- Improved error in logs on Fabric when Flywheel version is not supported
- Added Chinese translation thanks to AzureCrab

### 1.1.1

- Fixed the rendering issue with Create Fabric 6.0.7
- Fixed the "no active program" issue with dropped items and Just Stargate Mod
- Fixed the colored lights with Forgified Fabric
- Added missing Iris uniforms
- Flywheel is now an optional dependency  
Colorwheel can be installed without a Flywheel mod
- Set correct homepage, sources and issues page links in mod description

### 1.1.0

- Added fallback mode for non-supported shaderpacks  
This mode will force usage of Colorwheel by relying on existing shaders. This is the method used by Iris Flywheel Compat.  
This doesn't replace compatible packs (there WILL BE rendering issues) but it's better than nothing.
- Improved incompatible pack alert wording
- Added "Disable this alert" clickable text for incompatible pack and broken pack alerts
- `gl_MultiTexCoord1`/`gl_MultiTexCoord2` is now scaled by 240 to match Iris behavior
- Fixed old lightning multiplying the alpha channel
- Added namespace prefix to Flywheel shader functions related to old lighting
- Fixed config not being saved

### 1.0.0

- Public and documented shaderpack extension  
Shaderpack devs can now make their pack compatible
- Geometry shaders are now supported
- Provided mc_Entity values for terrain-like geometries  
This makes Iris Flywheel Compat incompatible with Colorwheel
- Provided correct at_midBlock values
- Missing Iris uniforms are now provided (fixes generated normals)
- Shaderpack programs are now pre-transformed (improves compile time)
- OIT is now disabled on Apple Silicon (fixes issues related to translucent geometries)
- Fixed weird rendering with colored shadows
- A lot of bug / incorrect behavior fixes

### 0.2.3

- Fixed lag when mining Create blocks and assembling contraptions
- Fixed incompatible shaderpack alert displaying full path
- Working Fabric builds

### 0.2.2

- Fixed compilation error when using Create: Enchantable Machinery
- Graphical issues may occur depending on the shaderpack used.   
A proper fix is being worked on.
- Added message to recommend using the patched shaderpack (if available) when an incompatible pack is used
- Fixed spam when an incompatible shaderpack is used
- Added config option to disable alerts when using an incompatible or broken shaderpack   
You can edit the config using /colorwheel or using Create mod config menu.

### 0.2.1

- Fixeed translucent waterwheels with Euphoria Patches

### 0.2.0

- Fixed crash when using zipped shaders
- Added support of light smoothness
- Fixed crash when Iris could not load because of an invalid shaderpack
- Shaderpacks can disable blending on specific buffers
- Added a separate gbuffers program to render translucents block entities / entities 
- Translucents are now rendered in the correct render stage
- Added support of blockEntityId and entityId uniforms
- Added comment directive to disable automatic flw_fragColor assignment

### 0.1.0

The first usable version.

Lag spikes when compiling shaders and minor graphical issus are to be expected.

To install a patch: extract the correct .zip into the shaderpack.
