# INTER VLAN 
Inter Vlan is a floodlight module that makes differents hosts in same network but having different VLAN id communicate each other.

### Things to Remember
This is a important point while creating match is if you are matching a field you should also match its prerequisite, like if you are matching ARP_TPA then you should also match ETH_TYPE, otherwise you will get an OFPBMC_PREREQ error message from switch and switch will not accept the flow.
In a table highest priority match is executed only, if we want to execetue more actions from different rules we can write those actions in different OF_Table and put a go_to instruction in instruction set to reach that table in which again matching will be done on basis of fields and priority.
Use ovs-ofctl dump-flows to check flows in OVS and wireshark/tcpdump to check packet received by host.

### Result
Hosts in same network but in different VLAN id can communicate using SDN if we manage the VLAN ID carefully.

Our Learning switch module install flows based on destination mac only and does not match vlan id for it, however it is very easy to use standard learning switch module with our module just make sure it installs rule in table-1 of openflow switch.
