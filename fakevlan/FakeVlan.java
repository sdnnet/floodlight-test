package net.floodlightcontroller.floodlightTest.fakevlan;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFFlowAdd;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFPacketIn;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.action.OFActionSetField;
import org.projectfloodlight.openflow.protocol.action.OFActions;
import org.projectfloodlight.openflow.protocol.instruction.OFInstruction;
import org.projectfloodlight.openflow.protocol.instruction.OFInstructionApplyActions;
import org.projectfloodlight.openflow.protocol.instruction.OFInstructions;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.protocol.oxm.OFOxms;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.OFBufferId;
import org.projectfloodlight.openflow.types.OFVlanVidMatch;
import org.projectfloodlight.openflow.types.TableId;
import org.python.antlr.PythonParser.return_stmt_return;
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
		
		return false;
	}
	Match createMatchFromPacket(IOFSwitch sw,OFPacketIn pi) {
		OFFactory myFactory = sw.getOFFactory();
		Match match = myFactory.buildMatch().setExact(MatchField.ETH_TYPE, EthType.IPv4).build();
		return match;
	}
	void writeFlowMod(Match m,IOFSwitch sw){
		OFFactory factory = sw.getOFFactory();
		OFActions actions = factory.actions();
		OFOxms oxms = factory.oxms();
		OFActionSetField fieldAction = actions.buildSetField().setField(oxms.buildVlanVid().setValue(OFVlanVidMatch.ofRawVid((short)100)).build()).build();
		ArrayList<OFAction> actionList = new ArrayList<>();
		actionList.add(fieldAction);
		OFInstructions instructions = factory.instructions();
		OFInstructionApplyActions applyActions = instructions.buildApplyActions().setActions(actionList).build();
		ArrayList<OFInstruction> insList = new ArrayList<>();
		insList.add(applyActions);
		OFFlowAdd mod = factory.buildFlowAdd().setBufferId(OFBufferId.NO_BUFFER).setHardTimeout(0).setIdleTimeout(0).setMatch(m).setPriority(134).setInstructions(insList).build();
		sw.write(mod);
	}
	@Override
	public Command receive(IOFSwitch sw, OFMessage msg, FloodlightContext cntx) {
		
		OFPacketIn vmsg = (OFPacketIn)msg;
		Ethernet eth = IFloodlightProviderService.bcStore.get(cntx, IFloodlightProviderService.CONTEXT_PI_PAYLOAD);
		eth.setVlanID((short) 100);
		Match match = createMatchFromPacket(sw,vmsg);
		//writeFlowMod(match,sw);
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
