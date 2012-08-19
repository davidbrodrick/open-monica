This is the Readme for the generic open-monica data server "ISServer.pl" and the associated example script "Gather_Sample_Data.pl"

The general architecture is simple:
- Gather_Sample_Data.pl runs and collects data which it stores to a directory.
- ISServer.pl listens on a network socket and delivers the contents of those files to whoever connects.

For more details on each task, refer to the ISServer.pl and Gather_Sample_Data.pl scripts.

To get open-monica to read your data, you further need to add the ISServer data source to the monitor-sources.txt file by simply adding this line:
atnf.atoms.monicalocal.externalsystem.ISServer 127.0.0.1:7111:1000

Then add the points you're gathering to the monitor-points.txt file. An example is provided here:

#The first one is the full ASCII data line, it is only looked at by other points
hidden.raw           "Raw ISServer Data"                             ""         ""            someserver_serverinfo T    Generic-"127.0.0.1:7111"         - -                             -     -              1000000 -
comp.CPULoad         "CPU load"                                      ""         ""           someserver   T    Listen-"someserver_serverinfo.hidden.raw"     - NV-"data_system-CPU1"  Range-"0""5.0"    Change-      1000000 -
comp.CPULoad2        "CPU load 2"                                    ""         ""           someserver   T    Listen-"someserver_serverinfo.hidden.raw"     - NV-"data_system-CPU2"             -     Change-      1000000 -
comp.CPULoad3        "CPU load 3"                                    ""         ""           someserver   T    Listen-"someserver_serverinfo.hidden.raw"     - NV-"data_system-CPU3"             -     Change-      1000000 -
comp.CPUAlarm        "CPU Alarm"                                     ""         ""           someserver   T    -  - {Calculation-"1""someserver.comp.CPULoad""a>5.0", NumberToBool-, EmailOnChange-"balt@inside.net""monica@inside.net""someserver CPU Alert = $V""someserver CPU alert transitioned to '$V' at $T UTC."}     StringMatch-"false""true"     {Change-}      10000000 -
comp.LoggedInUsers   "Number of users"                               ""         ""           someserver   T    Listen-"someserver_serverinfo.hidden.raw"     - NV-"data_system-Users"             -     Change-      1000000 -
net.PacketsIn        "Packets In"                                    ""         ""           someserver   T    Listen-"someserver_serverinfo.hidden.raw"     - {NV-"data_network-Packets_In",Delta-,EQ-"-x"}       -    {Change-}      1000000 -
net.PacketsOut       "Packets Out"                                   ""         ""           someserver   T    Listen-"someserver_serverinfo.hidden.raw"     - {NV-"data_network-Packets_Out",Delta-,EQ-"-x"}      -    {Change-}      1000000 -
net.TCPClientCount   "Established TCP Connections"                   ""         ""           someserver   T    Listen-"someserver_serverinfo.hidden.raw"     - NV-"data_network-Established_TCP_Connections"      -     {Change-}      1000000 -
disk.inode.disk1     "Disk 1 free inodes"                            ""         ""           someserver   T    Listen-"someserver_serverinfo.hidden.raw"     - NV-"data_diskfree-disk1_inodes"      -     {Change-}      60000000 -
disk.inode.disk2     "Disk 2 free inodes"                            ""         ""           someserver   T    Listen-"someserver_serverinfo.hidden.raw"     - NV-"data_diskfree-disk2_inodes"      -     {Change-}      60000000 -
disk.inode.disk3     "Disk 3 free inodes"                            ""         ""           someserver   T    Listen-"someserver_serverinfo.hidden.raw"     - NV-"data_diskfree-disk3_inodes"      -     {Change-}      60000000 -
disk.blocks.disk1    "Disk 1 free blocks"                            ""         ""           someserver   T    Listen-"someserver_serverinfo.hidden.raw"     - NV-"data_diskfree-disk1_blocks"      -     {Change-}      60000000 -
disk.blocks.disk2    "Disk 2 free blocks"                            ""         ""           someserver   T    Listen-"someserver_serverinfo.hidden.raw"     - NV-"data_diskfree-disk2_blocks"      -     {Change-}      60000000 -
disk.blocks.disk3    "Disk 3 free blocks"                            ""         ""           someserver   T    Listen-"someserver_serverinfo.hidden.raw"     - NV-"data_diskfree-disk3_blocks"      -     {Change-}      60000000 -
disk.gigs.disk1      "Disk 1 free GB"                                ""         "GB"         someserver   T    Listen-"someserver.disk.blocks.disk1"         - {EQ-"x*512/1024/1024/1024",NumDecimals-"3"}      -     {Change-}      60000000 -
disk.gigs.disk2      "Disk 2 free GB"                                ""         "GB"         someserver   T    Listen-"someserver.disk.blocks.disk2"         - {EQ-"x*512/1024/1024/1024",NumDecimals-"3"}      -     {Change-}      60000000 -
disk.gigs.disk3      "Disk 3 free GB"                                ""         "GB"         someserver   T    Listen-"someserver.disk.blocks.disk3"         - {EQ-"x*512/1024/1024/1024",NumDecimals-"3"}      -     {Change-}      60000000 -
disk.alarm           "Disk Alarm"                                    ""         ""           someserver   T    -  - {Calculation-"3""someserver.disk.gigs.disk1""someserver.disk.gigs.disk2""someserver.disk.gigs.disk3""a<50||b<50||c<50", NumberToBool-, EmailOnChange-"your@email.com""monica@yourmonitorserver.net""someserver Disk Alert = $V""someserver disk alert transitioned to '$V' at $T UTC."}     StringMatch-"false""true"     {Change-}      60000000 -
