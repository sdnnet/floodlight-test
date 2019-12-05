package net.floodlightcontroller.floodlightTest.intervlan;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFFlowAdd;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFPacketIn;
import org.projectfloodlight.openflow.protocol.OFPacketOut;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.action.OFActions;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.MacAddress;
import org.projectfloodlight.openflow.types.OFBufferId;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.TableId;
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
import net.floodlightcontroller.util.OFMessageUtils;

public class LearningSwitch implements IFloodlightModule, IOFMessageListener {
	protected static Logger log = LoggerFactory.getLogger(LearningSwitch.class);
	protected IFloodlightProviderService floodlightProviderService;
	protected HashMap<MacAddress,OFPort> map;
	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return "Learning Switch";
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
	void writeFlowMod(IOFSwitch sw,Match match,OFPort port){
		OFFactory factory = sw.getOFFactory();
		OFActions actions = factory.actions();
		OFAction outputAction = actions.buildOutput().setPort(port).build();
		ArrayList<OFAction> list = new ArrayList<>();
		list.add(outputAction);
		OFFlowAdd flow = factory.buildFlowAdd().setTableId(TableId.of(1))
			.setHardTimeout(0)
			.setIdleTimeout(0)
			.setActions(list)
			.setMatch(match)
			.setPriority(1)
			.setBufferId(OFBufferId.NO_BUFFER)
			.build();
		sw.write(flow);
	}	
	Match createMatch(IOFSwitch sw,MacAddress mac){
		OFFactory factory = sw.getOFFactory();
		return factory.buildMatch().setExact(MatchField.ETH_DST,mac).build();
	}

	void pushPacket(OFPacketIn pi,IOFSwitch sw,OFPort outport){
		if (pi == null) {
			return;
		}

		OFPort inPort = (pi.getVersion().compareTo(OFVersion.OF_12) < 0 ? pi.getInPort() : pi.getMatch().get(MatchField.IN_PORT));
		OFPacketOut.Builder pob = sw.getOFFactory().buildPacketOut();
		List<OFAction> actions = new ArrayList<OFAction>();
		actions.add(sw.getOFFactory().actions().buildOutput().setPort(outport).setMaxLen(0xffFFffFF).build());
		pob.setActions(actions);
		if (sw.getBuffers() == 0) {
			pi = pi.createBuilder().setBufferId(OFBufferId.NO_BUFFER).build();
			pob.setBufferId(OFBufferId.NO_BUFFER);
		} else {
			pob.setBufferId(pi.getBufferId());
		}

		OFMessageUtils.setInPort(pob, inPort);
		if (pi.getBufferId() == OFBufferId.NO_BUFFER) {
			byte[] packetData = pi.getData();
			pob.setData(packetData);
		}
		sw.write(pob.build());
	}

	@Override
	public Command receive(IOFSwitch sw, OFMessage msg, FloodlightContext cntx) {
		OFPacketIn pi = (OFPacketIn) msg;
		OFPort inPort = (pi.getVersion().compareTo(OFVersion.OF_12) < 0 ? pi.getInPort() : pi.getMatch().get(MatchField.IN_PORT));
		Ethernet eth = IFloodlightProviderService.bcStore.get(cntx, IFloodlightProviderService.CONTEXT_PI_PAYLOAD);
		map.put(eth.getSourceMACAddress(),inPort);
		OFPort outPort = map.get(eth.getDestinationMACAddress());
		if(eth.getDestinationMACAddress().equals(MacAddress.BROADCAST)){
			Match match = createMatch(sw,eth.getDestinationMACAddress());
			writeFlowMod(sw,match,OFPort.FLOOD);
			pushPacket(pi,sw,OFPort.FLOOD);
			return Command.CONTINUE;
		}else if(outPort == null){
			pushPacket(pi,sw,OFPort.FLOOD);
			return Command.CONTINUE;
		}else{
			Match match = createMatch(sw,eth.getDestinationMACAddress());
			writeFlowMod(sw,match,outPort);
			pushPacket(pi,sw,outPort);
			return Command.CONTINUE;
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
		floodlightProviderService = context.getServiceImpl(IFloodlightProviderService.class);
		map = new HashMap<>();
	}

	@Override
	public void startUp(FloodlightModuleContext context) throws FloodlightModuleException {
		// TODO Auto-generated method stub
		floodlightProviderService.addOFMessageListener(OFType.PACKET_IN, this);
	}
}

