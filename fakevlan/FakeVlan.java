package net.floodlightcontroller.floodlightTest.fakevlan;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFFlowAdd;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFPacketIn;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.protocol.oxm.OFOxms;
import org.projectfloodlight.openflow.types.OFBufferId;
import org.projectfloodlight.openflow.types.OFVlanVidMatch;
import org.projectfloodlight.openflow.types.TableId;
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
import net.floodlightcontroller.packet.Ethernet;

public class FakeVlan implements IFloodlightModule , IOFMessageListener{

	protected static Logger log = LoggerFactory.getLogger(FakeVlan.class);
	protected IFloodlightProviderService floodlightProviderService;
	@Override
	public String getName() {
		return "Fake Vlan";
	}

	@Override
	public boolean isCallbackOrderingPrereq(OFType type, String name) {
		return false;
	}

	@Override
	public boolean isCallbackOrderingPostreq(OFType type, String name) {
		
		return name=="PrintVlan";
	}
	Match createMatchFromPacket(IOFSwitch sw,OFPacketIn pi,Ethernet eth) {
		OFFactory myFactory = sw.getOFFactory();
		Match match = myFactory.buildMatch()
			.setExact(MatchField.VLAN_VID,OFVlanVidMatch.ofVlanVid(VlanVid.ofVlan(eth.getVlanID())))
			//.setExact(MatchField.ETH_DST,eth.getSourceMACAddress())
			.build();
		return match;
	}
	void writeFlowMod(Match m,IOFSwitch sw){
		OFFactory factory = sw.getOFFactory();
		OFOxms oxms = sw.getOFFactory().oxms();
		List<OFAction> al = new ArrayList<OFAction>();
		OFAction vlanAction = factory.actions().setField(oxms.vlanVid(OFVlanVidMatch.ofVlanVid(VlanVid.ofVlan(100))));
		al.add(vlanAction);
		OFFlowAdd flowAdd = sw.getOFFactory().buildFlowAdd()
			.setBufferId(OFBufferId.NO_BUFFER)
			.setHardTimeout(0)
			.setIdleTimeout(10)
			.setPriority(1)
			.setMatch(m)
			.setActions(al)
			.setTableId(TableId.of(1))
			.build();
		// and write it out
		sw.write(flowAdd);
	}
	@Override
	public Command receive(IOFSwitch sw, OFMessage msg, FloodlightContext cntx) {
		
		OFPacketIn vmsg = (OFPacketIn)msg;
		Ethernet eth = IFloodlightProviderService.bcStore.get(cntx, IFloodlightProviderService.CONTEXT_PI_PAYLOAD);
		eth.setVlanID((short) 100);
		Match match = createMatchFromPacket(sw,vmsg,eth);
		writeFlowMod(match,sw);
		return Command.CONTINUE;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleServices() {
		
		return null;
	}

	@Override
	public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
		
		return null;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
		
		return null;
	}

	@Override
	public void init(FloodlightModuleContext context) throws FloodlightModuleException {
		
		floodlightProviderService = context.getServiceImpl(IFloodlightProviderService.class);
	}

	@Override
	public void startUp(FloodlightModuleContext context) throws FloodlightModuleException {
		
		floodlightProviderService.addOFMessageListener(OFType.PACKET_IN, this);
	}
	
}
