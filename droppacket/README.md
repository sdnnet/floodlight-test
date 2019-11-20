# DROP PACKET
This module will install flow rule to **drop packets** coming from a **specified host**.
This is intended for information i.e How to drop a Packet.

## IDEA BEHIND HOW TO DROP A PACKET
For Droping a packet as specified in the Openflow Specifications we should **leave action and instruction list empty**.

## Things To Remember
If we do not define a action corresponding to a Packet for which we have created a match and passed it to writeflowmod() function. Then the switch drops it.

## Result
Here while testing this module there were two more hosts having Ip's **host1-10.4.0.2** and **host2-10.4.0.1**.
In this module host having **badIP i.e "10.4.0.3"** is **not allowed to communicate with other hosts** in the network i.e with host1 and host2.
This is beacuse every packet that has the source Ip address as badIp is being dropped.


