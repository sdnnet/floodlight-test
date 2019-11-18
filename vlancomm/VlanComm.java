package net.floodlightcontroller.floodlightTest.vlancomm;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFFlowAdd;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFPacketIn;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.protocol.oxm.OFOxms;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.OFBufferId;
import org.projectfloodlight.openflow.types.OFVlanVidMatch;
import org.projectfloodlight.openflow.types.TableId;
import org.projectfloodlight.openflow.types.U64;
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
	Match createMatchFromPacketARP(IOFSwitch sw,OFPacketIn pin,IPv4Address addr,VlanVid vid){
		OFFactory myFactory = sw.getOFFactory();
		Match match = myFactory.buildMatch().setExact(MatchField.ETH_TYPE,EthType.ARP)
			.setExact(MatchField.ARP_TPA, addr)
			.setExact(MatchField.VLAN_VID,OFVlanVidMatch.ofVlanVid(vid))
			.build();
		return match;
	}
	Match createMatchFromPacket(IOFSwitch sw,OFPacketIn pin,IPv4Address addr,VlanVid vid){
		OFFactory myFactory = sw.getOFFactory();
		Match match = myFactory.buildMatch().setExact(MatchField.ETH_TYPE,EthType.IPv4)
			.setExact(MatchField.IPV4_DST, addr)
			.setExact(MatchField.VLAN_VID,OFVlanVidMatch.ofVlanVid(vid))
			.build();
		return match;
	}
	void writeFlowMod(Match match,IOFSwitch sw,IPv4Address dstIp){
		OFFactory myFactory = sw.getOFFactory();
		OFOxms oxms = sw.getOFFactory().oxms();
		List<OFAction> actionList = new ArrayList<OFAction>();
		log.info("ID: {}",ipToVlanMap.get(dstIp).getVlan());
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
				.setHardTimeout(0)
				.setIdleTimeout(0)
				.setBufferId(OFBufferId.NO_BUFFER)
				.setActions(actionList)
				.setPriority(20000)
				.setTableId(TableId.of(3))
				.setCookie(U64.of(14312))
				.build();
		sw.write(flowadd);		
	}
	@Override
	public Command receive(IOFSwitch sw, OFMessage msg, FloodlightContext cntx) {
		// TODO Auto-generated method stub
		OFPacketIn vmsg = (OFPacketIn)msg;
		Ethernet eth = IFloodlightProviderService.bcStore.get(cntx, IFloodlightProviderService.CONTEXT_PI_PAYLOAD);
		VlanVid srcVlanId = VlanVid.ofVlan(eth.getVlanID());
		log.info("Packet recieved: {}",eth.getEtherType());
		if(eth.getEtherType().equals(EthType.IPv4)){
			log.info("IP PACKET RECEIVED");
			IPv4Address srcIp = ((IPv4)eth.getPayload()).getSourceAddress();
			IPv4Address dstIp = ((IPv4)eth.getPayload()).getDestinationAddress();
			if(!ipToVlanMap.containsKey(srcIp)){
				ipToVlanMap.put(srcIp,srcVlanId);
			}
			if(ipToVlanMap.containsKey(dstIp)){
//				eth.setVlanID(ipToVlanMap.get(dstIp).getVlan());
				Match match = createMatchFromPacket(sw,vmsg,dstIp,srcVlanId);
				writeFlowMod(match,sw,dstIp);
				log.info("WRITTEN PACKET");
				return Command.CONTINUE;
			}
			else{
				return Command.STOP;
			}
		}else if(eth.getEtherType().equals(EthType.ARP)){
			ARP arp = (ARP) eth.getPayload();
			IPv4Address srcIP = arp.getSenderProtocolAddress();
			IPv4Address dstIP = arp.getTargetProtocolAddress();
			if(!ipToVlanMap.containsKey(srcIP)){
				ipToVlanMap.put(srcIP,srcVlanId);
			}
			if(ipToVlanMap.containsKey(dstIP)){
//				eth.setVlanID(ipToVlanMap.get(dstIP).getVlan());
				Match match = createMatchFromPacketARP(sw,vmsg,dstIP,srcVlanId);
				log.info("Source IP : {}",srcIP);
				log.info("Destination IP: {}",dstIP);
				log.info("ARP PACKET RECEIVED");
				log.info("target protocol: {}",arp.getTargetProtocolAddress());
				log.info("vlan id: {}",eth.getVlanID());
				writeFlowMod(match,sw,dstIP);
				return Command.CONTINUE;
			}
			else{
				return Command.STOP;
			}
		}
		return Command.CONTINUE;
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
}
