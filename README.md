# XPerm
Grant permissions depending on level - Minecraft Bukkit Plugin

### Links
[Download](http://dragorn421.fr/xperm/XPerm.jar)  
[Aide en français](http://dragorn421.fr/xperm/)

### Commands
/xperm reload - Reload configuration  
Permission : xperm.xperm (given to op by default)

### Configuration
```
# Please check the logs after modifying the configuration, there may
# be warnings about wrongly written numbers or some other problems
permissions:
  # Minimum level to get associated permissions
  '421':
  # Granted permissions list
  - my.permission.1
  - my.permission.2
  - and.so.on
  # There are two ways to set the minimum level, here is the other one
  level-4210:
  - my.permission.1.bis
  - my.permission.2.bis
  - and.so.on.bis
```

### Other
The `xperm.ignore` permission (given to nobody by default) prevents the level of a player to change its permissions. Giving this permission
while the player is online may have unwnated effects until the player logs off and back in.  
Even if you should not use /reload, the plugin won't suffer from it and will work as usual, players won't have to relog to get their permissions.