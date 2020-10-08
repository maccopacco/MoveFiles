# MoveFiles

Quick script that parses a Config file of structure
```
mkv,txt                       //list of file extensions
15                            //epsilon in minutes to identify class
yyyy-MM-dd hh-mm-ss           //input date format from OBS
M-dd-yyyy                     //output date format
PHYS114,09:05:00,1,2,4,5      //Name of class, start time in (hh:mm:ss),days of week class occurs (1 = monday) 
LS201,10:15:00,2,5
MATH102,15:35:00,1,2,4,5
CS231,13:20:00,2,5
PHYS115,15:35:00,3
```

and moves video recordings of Kettering classes into appropriate folders by 
* name of file (start of recording time by OBS)
* command line arguments for renaming

```MoveFiles.jar PHYS214 PHYS224``` moves files from PHYS214 folder to PHYS224 folder
  
