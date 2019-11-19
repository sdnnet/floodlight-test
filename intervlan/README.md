# INTER VLAN 
Inter Vlan is a floodlight module that makes differents hosts in same network but in same VLAN id communicate each other.

### Things to Remember
This is a important point while creating match is if you are matching a field you should also match its prerequisite, like if you are matching ARP_TPA then you should also match ETH_TYPE, otherwise you will get an OFPBMC_PREREQ error message from switch and switch will not accept the flow.

### Result
Hosts in same network but in different VLAN id can communicate using SDN if we manage the VLAN ID carefully.

