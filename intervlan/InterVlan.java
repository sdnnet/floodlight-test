package net.floodlightcontroller.floodlightTest.intervlan;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFFlowAdd;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFPacketIn;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.action.OFActions;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.protocol.oxm.OFOxms;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.OFBufferId;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.OFVlanVidMatch;
import org.projectfloodlight.openflow.types.TableId;
import org.projectfloodlight.openflow.types.U128;
import org.projectfloodlight.openflow.types.VlanVid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.packet.ARP;
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.IPv4;

public class InterVlan implements IFloodlightModule, IOFMessageListener {
	protected static Logger log = LoggerFactory.getLogger(InterVlan.class);
	protected IFloodlightProviderService floodlightProviderService;
	protected HashMap<IPv4Address,Short> map;
	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return "InterVlan";
	}

	@Override
	public boolean isCallbackOrderingPrereq(OFType type, String name) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isCallbackOrderingPostreq(OFType type, String name) {
		// TODO Auto-generated method stub
		return name.equals("Forwarding");
	}
	private Match createArpMatch(IOFSwitch sw,short vid,IPv4Address destIp){
		OFFactory factory =  sw.getOFFactory();
		return factory.buildMatch().setExact(MatchField.VLAN_VID,OFVlanVidMatch.ofVlanVid(VlanVid.ofVlan(vid)))
			.setExact(MatchField.ETH_TYPE,EthType.ARP)
			.setExact(MatchField.ARP_TPA,destIp)
			.build();
	}

	private Match createIpMatch(IOFSwitch sw,short vid, IPv4Address destIp){
		OFFactory factory = sw.getOFFactory();
		return factory.buildMatch().setExact(MatchField.VLAN_VID,OFVlanVidMatch.ofVlanVid(VlanVid.ofVlan(vid)))
			.setExact(MatchField.ETH_TYPE,EthType.IPv4)
			.setExact(MatchField.IPV4_DST,destIp)
			.build();
	}
	private void writeFlowMod(IOFSwitch sw,Match match,short vid){
		OFFactory factory = sw.getOFFactory();
		OFOxms oxms = factory.oxms();
		OFActions actions = factory.actions();
		ArrayList<OFAction> actionList = new ArrayList<>();
		OFAction vidAction = actions.buildSetField()
			.setField(
			oxms.buildVlanVid()
			.setValue(OFVlanVidMatch.ofVlan(vid))
			.build()
			)
			.build();
		actionList.add(vidAction);

		OFFlowAdd flowAdd = factory.buildFlowAdd()
			.setHardTimeout(0)
			.setBufferId(OFBufferId.NO_BUFFER)
			.setIdleTimeout(0)
			.setActions(actionList)
			.setPriority(1000)
			.setMatch(match)
			.build();
		sw.write(flowAdd);
	}
	@Override
	public Command receive(IOFSwitch sw, OFMessage msg, FloodlightContext cntx) {
		// TODO Auto-generated method stub
		Ethernet eth = IFloodlightProviderService.bcStore.get(cntx, IFloodlightProviderService.CONTEXT_PI_PAYLOAD);
		short vid = eth.getVlanID();
		EthType ethType = eth.getEtherType();
		if(ethType.equals(EthType.ARP)){
			ARP arp = (ARP) eth.getPayload();
			IPv4Address srcIp  = arp.getSenderProtocolAddress();
			IPv4Address destIp = arp.getTargetProtocolAddress();
			if(!map.containsKey(srcIp)){
				map.put(srcIp,vid);
			}
			if(!map.containsKey(destIp)){
				return Command.STOP;
			}
			Match match = createArpMatch(sw,vid,destIp);
			writeFlowMod(sw,match,map.get(destIp));
			eth.setVlanID(vid);
			return Command.CONTINUE;
		}else if(ethType.equals(EthType.IPv4)){
			IPv4 ip = (IPv4) eth.getPayload();
			IPv4Address srcIp = ip.getSourceAddress();
			IPv4Address destIp = ip.getDestinationAddress();
			if(!map.containsKey(srcIp)){
				map.put(srcIp,vid);
			}
			if(!map.containsKey(destIp)){
				return Command.STOP;
			}
			Match match = createIpMatch(sw,vid,destIp);
			writeFlowMod(sw,match,map.get(destIp));
			return Command.CONTINUE;
		}else return Command.CONTINUE;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleServices() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void init(FloodlightModuleContext context) throws FloodlightModuleException {
		// TODO Auto-generated method stub
		floodlightProviderService = context.getServiceImpl(IFloodlightProviderService.class);
		map = new HashMap<>();
	}

	@Override
	public void startUp(FloodlightModuleContext context) throws FloodlightModuleException {
		// TODO Auto-generated method stub
		floodlightProviderService.addOFMessageListener(OFType.PACKET_IN, this);
	}
}

