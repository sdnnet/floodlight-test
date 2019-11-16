# Fake VLAN
This module will modify packets on the fly to make them in vlan id 100.
This is intended to test different network devices in same vlan.

### Things to Remember
If you are changing a field 'x' using OFFlowMod then you should also have a match for that field inthe OFFlowMod packet otherwise switch would not accept that flowMOd and give an OFET_BAD_ACTION and OFBEC_MATCH_INCONSISTENT error.


### Result
Inspite of 2 devices in same vlan but if they are in different network they can not communicate.
(Atleast in floodlight)

But if they are in same network achieved by creating vlan network interfaces then they can communicate.

But with this module we can guarantee that if 2 devices are in same network but in different vlan then we can make them communicate by installing appropriate rules on switches similar to what is demonstrated here.
