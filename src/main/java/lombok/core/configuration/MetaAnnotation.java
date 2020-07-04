package lombok.core.configuration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class MetaAnnotation implements ConfigurationValueType {
	private Annotation annotation;
	private List<Annotation> targetAnnotations;
	
	private MetaAnnotation() {
	}
	
	public static MetaAnnotation valueOf(String value) {
		String[] metaAnnotationString = value.split(" ");
		ArrayList<Annotation> annotations = new ArrayList<Annotation>();
		for (String string : metaAnnotationString) {
			try {
				annotations.add(parseAnnotationString(string));
			} catch (Exception e) {
				throw new IllegalArgumentException("Invalid meta annotation format", e);
			}
		}
		
		if (annotations.size() < 2) {
			throw new IllegalArgumentException("At least one target annotation is required");
		}
		
		MetaAnnotation meta = new MetaAnnotation();
		meta.annotation = annotations.remove(0);
		meta.targetAnnotations = annotations;
		
//		for (Annotation annotation : annotations) {
//			for (Argument argument : annotation.args) {
//				if (argument.type == Argument.class) {
//					boolean found = false;
//					for (Argument argument2 : meta.annotation.args) {
//						if (argument.value.equals(argument2.name)) {
//							argument.value = argument2.value;
//							argument.type = argument2.type;
//							found = true;
//						}
//					}
//					if (!found) {
//						throw new IllegalArgumentException("The bound annotation value <" + argument.value + "> is not set in the meta annotation" );
//					}
//				}
//			}
//		}
		
		return meta;
	}
	
	private static Annotation parseAnnotationString(String string) {
		if (string.startsWith("@")) {
			string = string.substring(1);
		}
		
		Annotation annotation = new Annotation();
		int argumentStart = string.indexOf("(");
		if (argumentStart == -1) {
			annotation.name = string;
			annotation.args = Collections.emptyList();
		} else {
			annotation.name = string.substring(0, argumentStart);
			annotation.args = new ArrayList<MetaAnnotation.Argument>();
			String argumentsString = string.substring(argumentStart + 1, string.length() - 1);
			String[] argumentStrings = argumentsString.split(",(?![^{]+})");
			for (String argumentString : argumentStrings) {
				Argument argument = parseArgumentString(argumentString);
				annotation.args.add(argument);
			}
		}
		return annotation;
	}
	
	private static Argument parseArgumentString(String string) {
		String name;
		String value;
		List<ArgumentValue> values = null;
		
		String[] parts = string.split("=");
		if (parts.length == 1 && parts.length == 1) {
			name = "value";
			value = parts[0];
		} else {
			name = parts[0];
			value = parts[1];
		}

		char firstChar = value.charAt(0);
		if (firstChar == '{') {
			value = value.substring(1, value.length() - 1);
			values = new ArrayList<ArgumentValue>();
			for (String arrayValue : value.split(",")) {
				values.add(parseArgumentValueString(arrayValue));
			}
		}
		
		Argument arg = new Argument();
		arg.name = name;
		if (values != null) {
			arg.values = values;
		} else {
			arg.value = parseArgumentValueString(value);
		}
		return arg;
	}
	
	private static ArgumentValue parseArgumentValueString(String value) {
		ArgumentValue av = new ArgumentValue();
		av.value = value;
		
		char firstChar = value.charAt(0);
		if (firstChar == '-' || Character.isDigit(firstChar)) {
			if (value.contains(".")) {
				av.type = ArgumentType.DOUBLE;
			} else {
				av.type = ArgumentType.INTEGER;
			}
		} else if (firstChar == '"') {
			av.type = ArgumentType.STRING;
			av.value = value.substring(1, value.length() - 1);
//		} else if (firstChar == '<') {
//			av.type = ArgumentType.BOUND;
//			value = value.substring(1, value.length() - 1);
//			String[] split = Arrays.copyOf(value.split("|"), 2);
//			bound = split[0];
//			value = split[1];
		} else {
			av.type = ArgumentType.REFERENCE;
		}
		return av;
	}
	
	public static String description() {
		return "";
	}
	
	public static String exampleValue() {
		return "";
	}
	
	public String getAnnotationName() {
		return annotation.name;
	}
	
	public Annotation getAnnotation() {
		return annotation;
	}
	
	public List<Annotation> getTargetAnnotations() {
		return targetAnnotations;
	}
	
	public static class Annotation {
		public String name;
		public List<Argument> args;
	}
	
	public static class Argument {
		public String name;
		public ArgumentValue value;
		public List<ArgumentValue> values;
	}
	
	public static class ArgumentValue {
		public ArgumentType type;
		public String value;
		public String bound;
	}
	
	public enum ArgumentType {
		DOUBLE, INTEGER, STRING, REFERENCE
	}
}
