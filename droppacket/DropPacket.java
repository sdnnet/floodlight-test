import java.util.Collection;
import java.util.Map;

import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFPacketIn;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.IPv4Address;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.packet.ARP;
import net.floodlightcontroller.packet.Ethernet;

public class DropPacket implements IFloodlightModule, IOFMessageListener {

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return "DropPacket";
	}

	@Override
	public boolean isCallbackOrderingPrereq(OFType type, String name) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isCallbackOrderingPostreq(OFType type, String name) {
		// TODO Auto-generated method stub
		return false;
	}
	Match createMatchFromPacket(IOFSwitch sw,OFPacketIn pi,IPv4Address badIp){
		OFFactory myfactory = sw.getOFFactory();
		Match match = myfactory.buildMatch().setExact(MatchField.ETH_TYPE,EthType.IPv4);
			.setExact(MatchField.IPV4_SRC,badIp)
			.build();
		return match;
	}
	Match createMatchFromPacketARP(IOFSwitch sw,OFPacketIn pi,IPv4Address badIp){
		OFFactory myfactory = sw.getOFFactory();
		Match match = myfactory.buildMatch().setExact(MatchField.ETH_TYPE,EthType.ARP);
		.setExact(MatchField.ARP_TPA,badIp)
		.build();
		return match;
	}
	void writeFlowMod(Match Match, IOFSwitch sw, IPv4Address badIp){
		OFOxms oxms = sw.getOFFactory().oxms();
		List<OFAction> actionList = new ArrayList<OFAction>();
		OFAction checkBadIp = myfactory.actions().buildSetField()
			.SetField(
			oxms.build
	}

		@Oblverride
		public Command receive(IOFSwitch sw, OFMessage msg, FloodlightContext cntx) {
			// TODO Auto-generated method stub
			OFPacketIn vmsg = (OFPacketIn)msg;
			Ethernet eth = IFloodlightProviderService.bcStore.get(cntx, IFloodlightProviderService.CONTEXT_PI_PAYLOAD);
			IPv4Address badIp = IPv4Address.of("10.4.0.3");
			if(eth.getEtherType().equals(EthType.ARP)){
				ARP arp = (ARP)eth.getPayload();
				IPv4Address srcIp = arp.getSenderProtocolAddress();
				if(srcIp.equals(badIp)){
					Match match = createMatchFromPacketARP(sw,vmsg,badIp);
					writeFlowMod(match,sw,badIp)
				}
				else return Command.CONTINUE;
			}
			else if(eth.getEtherType().equals(EthType.IPv4)){
				IPv4Address srcIP = ((IPv4)eth.getPayload()).getSourceAddress();
				if(srcIP.equals(badIp)){
					Match match = createMatchFromPacket(sw,vmsg,badIp);
					writeFlowMod(match,sw,badIp);
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
					//
					floodlightProviderService = context.getServiceImpl(IFloodlightProviderService.class);

				}

				@Override
				public void startUp(FloodlightModuleContext context) throws FloodlightModuleException {
					// TODO Auto-generated method stub

					floodlightProviderService.addOFMessageListener(OFType.PACKET_IN, this);

				}
			}

