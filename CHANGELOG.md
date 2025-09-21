### 1.0.0

- Public and documented shaderpack extension  
Shaderpack devs can now make their pack compatible
- Geometry shaders are now supported
- Provides mc_Entity values for terrain-like geometries  
This makes Iris Flywheel Compat incompatible with Colorwheel
- Provides correct at_midBlock values
- Missing Iris uniforms are now provided (fixes generated normals)
- Shaderpack programs are now pre-transformed (improves compile time)
- OIT is now disabled on Apple Silicon (fixes issues related to translucent geometries)
- Fixes weird rendering with colored shadows
- A lot of bug / incorrect behavior fixes

### 0.2.3

- Fixes lag when mining Create blocks and assembling contraptions
- Fixes incompatible shaderpack alert displaying full path
- Working Fabric builds

### 0.2.2

- Fixes compilation error when using Create: Enchantable Machinery
- Graphical issues may occur depending on the shaderpack used.   
A proper fix is being worked on.
- Adds message to recommend using the patched shaderpack (if available) when an incompatible pack is used
- Fixes spam when an incompatible shaderpack is used
- Adds config option to disable alerts when using an incompatible or broken shaderpack   
You can edit the config using /colorwheel or using Create mod config menu.

### 0.2.1

- Fixes translucent waterwheels with Euphoria Patches

### 0.2.0

- Fix crash when using zipped shaders
- Support light smoothness
- Fix crash when Iris could not load because of an invalid shaderpack
- Allow shaderpacks to disable blending on specific buffers
- Adds a separate gbuffers program to render translucents block entities / entities 
- Render translucents in the correct render stage
- Provide blockEntityId and entityId uniforms
- Add comment directive to disable automatic flw_fragColor assignment

### 0.1.0

The first usable version.

Lag spikes when compiling shaders and minor graphical issus are to be expected.

To install a patch: extract the correct .zip into the shaderpack.
