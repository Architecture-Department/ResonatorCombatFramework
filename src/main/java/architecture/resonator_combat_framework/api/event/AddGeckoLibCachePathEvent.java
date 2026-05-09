package architecture.resonator_combat_framework.api.event;

import net.neoforged.bus.api.Event;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AddGeckoLibCachePathEvent extends Event {
	private final List<String> modelPaths = new ArrayList<>();
	private final List<String> animationPaths = new ArrayList<>();

	public void addModelPath(String path) {
		modelPaths.add(path);
	}

	public List<String> getModelPaths() {
		return Collections.synchronizedList(modelPaths);
	}

	public void addAnimationPath(String path) {
		animationPaths.add(path);
	}

	public List<String> getAnimationPaths() {
		return Collections.synchronizedList(animationPaths);
	}
}
