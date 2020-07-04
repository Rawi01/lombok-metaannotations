package lombok.experimental;

import java.util.List;

import lombok.core.configuration.ConfigurationKey;
import lombok.core.configuration.ConfigurationKeysLoader;
import lombok.core.configuration.MetaAnnotation;

public class ConfigurationKeys implements ConfigurationKeysLoader {
	public static final ConfigurationKey<List<MetaAnnotation>> META_ANNOTATION = new ConfigurationKey<List<MetaAnnotation>>("lombok.metaAnnotations", "List of meta annos") {};
}