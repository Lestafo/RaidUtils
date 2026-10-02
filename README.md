
Raid Utils
=======

This is my first ~~(second, but we don't talk about the actual first)~~ mod for minecraft. Made due to a friend's request
Raid utils is intended to be a utility mod for server owners, allowing them to control and monitor different aspects of minecraft raids.
As of now, the mod is rather bare-bones, fulfilling only the specifics requested by my friend.   


## Features: ##

### /raidutils start
 ````
 Arguments:
  omen_lvl: integer equivalent of the Bad Omen effect the raid should have
  position: coordinates where the raid should take place. MUST be at/near a village as to not interfere with other raid mods   
  
 Starts a raid with the assigned level at the specified coordinates.
 The command returns a success message containing the ID of the raid. 
 ````
  


### /raidutils stop
````
Arguments: 
 RaidId: the ID of the raid.

It will stop an ongoing raid with the given ID.

````
Upon victory, the mod sends a server message indicating the raid has been victorious, which can then be parsed by an external script, allowing for customized rewards and events.

## Why

You might know by now that Minecraft already has (debug-level) commands for starting and stopping raids. In fact, my mod utilized the vanilla commands as a base.    
**HOWEVER**, the vanilla raid commands require a player to be standing at the position the raid will start in. This mod allows the server to remotely start a raid at any village.   
Vanilla raids also do not have a handy way of detecting when they are over and if anyone is victorious unless you count checking the hero of the village effect at every *X* seconds.


CONSIDERATIONS: 
==========
Keep in mind that this is my first mod and i'm not too familiar with the ecosystem nor with minecraft's own internal logic.   
The mod is in its early stages and is bound to change radically going into the future.   
Any feedback, requests, tips or suggestions are more than welcome.

PLANNED/UPCOMING FEATURES:
===========
- Greater customization of raids
- Integration with other raid mods
- Embedded loot table for raid rewards independent of external scripts
- Make raid command less dependent on player presence