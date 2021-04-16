package io.vertigo.ai.impl.bt.parser;

import java.util.List;

import io.vertigo.ai.bt.BTNodes;
import io.vertigo.ai.bt.BtNodeProvider;

/**
 * Default BT commands.
 * 
 * @author skerdudou
 */
public class DefaultBtTextParserPlugin extends SimpleBtTextParserPlugin<BtNodeProvider> {

	@Override
	public BtNodeProvider getNodeProvider(final List<Object> params) {
		// full stateless => no need of BtNodeProvider
		return null;
	}

	@Override
	protected void init() {
		registerStatelessCompositeCommand("sequence", (c, l) -> BTNodes.sequence(l));
		registerStatelessCompositeCommand("selector", (c, l) -> BTNodes.selector(l));
		registerStatelessCompositeCommand("try", (c, l) -> BTNodes.doTry(c.getIntParam(0), l));
		registerStatelessCompositeCommand("loop", (c, l) -> {
			final var optionalInt = c.getOptIntParam(0);
			if (optionalInt.isPresent()) {
				return BTNodes.loop(optionalInt.getAsInt(), l);
			}
			return BTNodes.loop(l);
		});
	}

}
