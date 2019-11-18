package net.floodlightcontroller.floodlightTest.vlancomm;

import java.util.concurrent.ConcurrentHashMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFFlowAdd;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFPacketIn;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.OFVlanVidMatch;
import org.projectfloodlight.openflow.types.VlanVid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.projectfloodlight.openflow.protocol.match.*;
import org.projectfloodlight.openflow.protocol.oxm.OFOxms;

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

public class VlanComm implements IFloodlightModule,IOFMessageListener{
	protected static Logger log = LoggerFactory.getLogger(VlanComm.class);

	
	protected IFloodlightProviderService floodlightProviderService;
	protected Map<IPv4Address,VlanVid> ipToVlanMap; 
	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return "VlanComm";
	}
	@Override
	public boolean isCallbackOrderingPrereq(OFType type, String name) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isCallbackOrderingPostreq(OFType type, String name) {
		// TODO Auto-generated method stub
		return true;
	}
	Match createMatchFromPacket(IOFSwitch sw,OFPacketIn pin,IPv4Address addr,VlanVid vid){
		OFFactory myFactory = sw.getOFFactory();
		Match match = myFactory.buildMatch().setExact(MatchField.IPV4_DST, addr)
			.setExact(MatchField.VLAN_VID,OFVlanVidMatch.ofVlanVid(vid))
			.build();
		return match;
	}
	void writeFlowMod(Match match,IOFSwitch sw,IPv4Address dstIp){
		OFFactory myFactory = sw.getOFFactory();
		OFOxms oxms = sw.getOFFactory().oxms();
		List<OFAction> actionList = new ArrayList<OFAction>();
		OFAction setDstVlan= myFactory.actions().buildSetField()
			.setField(
			oxms.buildVlanVid()
			.setValue(OFVlanVidMatch.ofVlanVid(ipToVlanMap.get(dstIp)))
			.build()
			)
			.build();
		actionList.add(setDstVlan);
		OFFlowAdd flowadd = sw.getOFFactory().buildFlowAdd()
				.setMatch(match)
				.setActions(actionList)
				.build();
		sw.write(flowadd);		
	}
	@Override
	public Command receive(IOFSwitch sw, OFMessage msg, FloodlightContext cntx) {
		// TODO Auto-generated method stub
		OFPacketIn vmsg = (OFPacketIn)msg;
		Ethernet eth = IFloodlightProviderService.bcStore.get(cntx, IFloodlightProviderService.CONTEXT_PI_PAYLOAD);
		VlanVid srcVlanId = VlanVid.ofVlan(eth.getVlanID());
		//VlanVid dstVlanId = VlanVid.ofVlan(eth.getVlanID());
		if(eth.getEtherType().equals(EthType.IPv4)){
			IPv4Address srcIp = ((IPv4)eth.getPayload()).getSourceAddress();
			ipToVlanMap.put(srcIp,srcVlanId);
			IPv4Address dstIp = ((IPv4)eth.getPayload()).getDestinationAddress();
			if(ipToVlanMap.containsKey(dstIp)){
				Match match = createMatchFromPacket(sw,vmsg,dstIp,srcVlanId);
				writeFlowMod(match,sw,dstIp);
				log.info("WRITTEN PACKET");
				return Command.CONTINUE;
			}
			else return Command.STOP;
		}else if(eth.getEtherType().equals(EthType.ARP)){
			ARP arp = (ARP) eth.getPayload();
			IPv4Address srcIP = arp.getSenderProtocolAddress();
			IPv4Address dstIP = arp.getTargetProtocolAddress();
			ipToVlanMap.put(srcIP,srcVlanId);
			if(ipToVlanMap.containsKey(dstIP)){
			Match match = createMatchFromPacket(sw,vmsg,dstIP,srcVlanId);
			writeFlowMod(match,sw,dstIP);
			log.info("ARP PACKET RECEIVED");
			return Command.CONTINUE;
			}
			else return Command.STOP;
		}
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
		ipToVlanMap = new ConcurrentHashMap<IPv4Address,VlanVid>();
		floodlightProviderService = context.getServiceImpl(IFloodlightProviderService.class);
	}

	@Override
	public void startUp(FloodlightModuleContext context) throws FloodlightModuleException {
		// TODO Auto-generated method stub
		floodlightProviderService.addOFMessageListener(OFType.PACKET_IN, this);

	}
