package net.floodlightcontroller.floodlightTest.fakevlan;

import java.util.*;

import org.projectfloodlight.openflow.protocol.*;
import org.projectfloodlight.openflow.protocol.action.*;
import org.projectfloodlight.openflow.protocol.instruction.*;
import org.projectfloodlight.openflow.protocol.match.*;
import org.projectfloodlight.openflow.protocol.oxm.*;
import org.projectfloodlight.openflow.types.*;
import org.python.antlr.PythonParser.return_stmt_return;
import org.slf4j.*;

import net.floodlightcontroller.core.*;
import net.floodlightcontroller.core.module.*;
import net.floodlightcontroller.packet.*;
import net.floodlightcontroller.util.FlowModUtils;

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
		OFFlowMod.Builder fmb = sw.getOFFactory().buildFlowAdd();
		OFOxms oxms = sw.getOFFactory().oxms();
		fmb.setMatch(m);
		fmb.setCookie((U64.of(this.hashCode())));
		fmb.setIdleTimeout(5);
		fmb.setHardTimeout(0);
		fmb.setPriority(134);
		fmb.setBufferId(OFBufferId.NO_BUFFER);
		List<OFAction> al = new ArrayList<OFAction>();
		OFAction vlanAction = factory.actions().setField(oxms.vlanVid(OFVlanVidMatch.ofVlanVid(VlanVid.ofVlan(100))));
	//	al.add(ethAction);
		al.add(vlanAction);
		OFFlowAdd flowAdd = sw.getOFFactory().buildFlowAdd()
			.setBufferId(OFBufferId.NO_BUFFER)
			.setHardTimeout(0)
			.setIdleTimeout(10)
			.setPriority(3276)
			.setMatch(m)
			.setActions(al)
			.setTableId(TableId.of(3))
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
