package lombok.javac.handlers;

import org.mangosdk.spi.ProviderFor;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCAnnotation;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCModifiers;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;

import lombok.core.HandlerPriority;
import lombok.core.configuration.MetaAnnotation;
import lombok.core.configuration.MetaAnnotation.Annotation;
import lombok.core.configuration.MetaAnnotation.Argument;
import lombok.core.configuration.MetaAnnotation.ArgumentType;
import lombok.core.configuration.MetaAnnotation.ArgumentValue;
import lombok.experimental.ConfigurationKeys;
import lombok.javac.JavacASTAdapter;
import lombok.javac.JavacASTVisitor;
import lombok.javac.JavacNode;
import lombok.javac.JavacTreeMaker;

@ProviderFor(JavacASTVisitor.class)
@HandlerPriority(-2^18)
public class HandleMetaAnnotations extends JavacASTAdapter {
	
	@Override public void visitType(JavacNode typeNode, JCClassDecl type) {
		handleMetaAnnotations(typeNode, type.mods, type);
	}
	
	@Override public void visitField(JavacNode fieldNode, JCVariableDecl field) {
		handleMetaAnnotations(fieldNode, field.mods, field);
	}
	
	private void handleMetaAnnotations(JavacNode node, JCModifiers mods, JCTree source) {
		if (mods.annotations.size() == 0) return;
		
		java.util.List<MetaAnnotation> metas = node.getAst().readConfiguration(ConfigurationKeys.META_ANNOTATION);
		JavacTreeMaker maker = node.getTreeMaker();
		for (JCAnnotation annotation : mods.annotations) {
			for (MetaAnnotation meta : metas) {
				if (meta.getAnnotationName().equals(annotation.annotationType.toString())) {
					for (Annotation targetAnnotation : meta.getTargetAnnotations()) {
						ListBuffer<JCExpression> args = new ListBuffer<JCExpression>();
						for (Argument argument : targetAnnotation.args) {
							JCExpression lhs = maker.Ident(node.toName(argument.name));
							JCExpression rhs;
							
							if (argument.values != null) {
								ListBuffer<JCExpression> valueExpressions = new ListBuffer<JCExpression>();
								for (ArgumentValue argumentValue : argument.values) {
									valueExpressions.add(toExpression(argumentValue, node));
								}
								rhs = maker.NewArray(null, List.<JCExpression>nil(), valueExpressions.toList());
							} else {
								rhs = toExpression(argument.value, node);
							}
							args.add(maker.Assign(lhs, rhs));
						}
						addAnnotation(mods, node, source, targetAnnotation.name, args.toList());
					}
				}
			}
		}
	}
	
	private JCExpression toExpression(ArgumentValue argument, JavacNode node) {
		JavacTreeMaker maker = node.getTreeMaker();

		JCExpression expression;
		if (argument.type == ArgumentType.DOUBLE) {
			expression = maker.Literal(Double.parseDouble(argument.value));
		} else if (argument.type == ArgumentType.INTEGER) {
			expression = maker.Literal(Integer.parseInt(argument.value));
		} else if (argument.type == ArgumentType.STRING) {
			expression = maker.Literal(argument.value);
//		} else if (argument.type == Argument.class) {
//			getAnnotationArgumentExpression(annotation.getArguments(), value);
//			rhs = maker.Literal(value);
		} else {
			expression = JavacHandlerUtil.chainDotsString(node, argument.value);
		}
		return expression;
	}
	
//	private JCExpression getAnnotationArgumentExpression(List<JCExpression> arguments, String value) {
//		for (JCExpression jcExpression : arguments) {
//			String lhs = "value";
//			JCExpression rhs = jcExpression;
//			if (jcExpression instanceof JCAssign) {
//				JCAssign jcAssign = (JCAssign) jcExpression;
//				lhs = jcAssign.lhs.toString();
//				rhs = jcAssign.rhs;
//			}
//			if (lhs.equals(value)) {
//				return rhs;
//			}
//		}
//		return null;
//	}

	private void addAnnotation(JCModifiers mods, JavacNode node, JCTree source, String annotationTypeFqn, List<JCExpression> argList) {
		if (JavacHandlerUtil.hasAnnotation(annotationTypeFqn, node)) return;
		
		JavacTreeMaker maker = node.getTreeMaker();
		JCExpression annType = JavacHandlerUtil.chainDotsString(node, annotationTypeFqn);
		JCAnnotation annotation = JavacHandlerUtil.recursiveSetGeneratedBy(maker.Annotation(annType, argList), source, node.getContext());
		mods.annotations = mods.annotations.append(annotation);
	}
}
