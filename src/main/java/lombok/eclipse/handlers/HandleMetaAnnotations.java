package lombok.eclipse.handlers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.Annotation;
import org.eclipse.jdt.internal.compiler.ast.ArrayInitializer;
import org.eclipse.jdt.internal.compiler.ast.DoubleLiteral;
import org.eclipse.jdt.internal.compiler.ast.Expression;
import org.eclipse.jdt.internal.compiler.ast.FieldDeclaration;
import org.eclipse.jdt.internal.compiler.ast.IntLiteral;
import org.eclipse.jdt.internal.compiler.ast.MarkerAnnotation;
import org.eclipse.jdt.internal.compiler.ast.MemberValuePair;
import org.eclipse.jdt.internal.compiler.ast.NormalAnnotation;
import org.eclipse.jdt.internal.compiler.ast.QualifiedTypeReference;
import org.eclipse.jdt.internal.compiler.ast.SingleMemberAnnotation;
import org.eclipse.jdt.internal.compiler.ast.StringLiteral;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.mangosdk.spi.ProviderFor;

import lombok.core.AST.Kind;
import lombok.core.HandlerPriority;
import lombok.core.configuration.MetaAnnotation;
import lombok.core.configuration.MetaAnnotation.Argument;
import lombok.core.configuration.MetaAnnotation.ArgumentType;
import lombok.core.configuration.MetaAnnotation.ArgumentValue;
import lombok.eclipse.Eclipse;
import lombok.eclipse.EclipseASTAdapter;
import lombok.eclipse.EclipseASTVisitor;
import lombok.eclipse.EclipseNode;
import lombok.experimental.ConfigurationKeys;

@ProviderFor(EclipseASTVisitor.class)
@HandlerPriority(Integer.MIN_VALUE)
public class HandleMetaAnnotations extends EclipseASTAdapter {
	@Override
	public void visitType(EclipseNode typeNode, TypeDeclaration type) {
		type.annotations = handleMetaAnnotation(typeNode, type.annotations);
	}
	
	@Override public void visitField(EclipseNode fieldNode, FieldDeclaration field) {
		field.annotations = handleMetaAnnotation(fieldNode, field.annotations);
	}
	
	public Annotation[] handleMetaAnnotation(EclipseNode node, Annotation[] annotations) {
		if (annotations == null) return annotations;
		
		Annotation[] result = annotations;
		
		List<MetaAnnotation> metas = node.getAst().readConfiguration(ConfigurationKeys.META_ANNOTATION);
		for (Annotation annotation : annotations) {
			int pS = annotation.sourceStart, pE = annotation.sourceEnd;
			for (MetaAnnotation meta : metas) {
				if (meta.getAnnotationName().equals(annotation.type.toString())) {
					for (MetaAnnotation.Annotation targetAnnotation : meta.getTargetAnnotations()) {
						List<MemberValuePair> args = new ArrayList<MemberValuePair>();
						for (Argument argument : targetAnnotation.args) {
							Expression rhs;
							
							if (argument.values != null) {
								List<Expression> valueExpressions = new ArrayList<Expression>();
								for (ArgumentValue argumentValue : argument.values) {
									valueExpressions.add(toExpression(argumentValue, annotation));
								}
								
								ArrayInitializer arr = new ArrayInitializer();
								arr.expressions = valueExpressions.toArray(new Expression[0]);
								arr.sourceStart = pS;
								arr.sourceEnd = pE;
								arr.statementEnd = pE;
								rhs = arr;
							} else {
								rhs = toExpression(argument.value, annotation);
							}
							MemberValuePair memberValuePair = new MemberValuePair(argument.name.toCharArray(), 0, 0, rhs);
							args.add(memberValuePair);
						}
						result = addAnnotation(node, annotation, result, targetAnnotation.name, args.toArray(new MemberValuePair[0]));
					}
				}
			}
		}
		
		return result;
	}
	
	private Expression toExpression(ArgumentValue argument, Annotation source) {
		int pS = source.sourceStart, pE = source.sourceEnd;
		Expression expression;
		String value = argument.value;
		if (argument.type == ArgumentType.DOUBLE) {
			expression = new DoubleLiteral(value.toCharArray(), pS, pE);
		} else if (argument.type == ArgumentType.INTEGER) {
			expression = IntLiteral.buildIntLiteral(value.toCharArray(), pS, pE);
		} else if (argument.type == ArgumentType.STRING) {
			expression = new StringLiteral(value.toCharArray(), pS, pE, -1);
		} else {
			expression = EclipseHandlerUtil.createNameReference(argument.value, source);
		}
		return expression;
	}
	

	private Annotation[] addAnnotation(EclipseNode node, ASTNode source, Annotation[] originalAnnotationArray, String annotationTypeFqnString, ASTNode... args) {
		if (EclipseHandlerUtil.hasAnnotation(annotationTypeFqnString, node)) return originalAnnotationArray;
		
		char[][] annotationTypeFqn = Eclipse.fromQualifiedName(annotationTypeFqnString);
		
		int pS = source.sourceStart, pE = source.sourceEnd;
		long p = (long)pS << 32 | pE;
		long[] poss = new long[annotationTypeFqn.length];
		Arrays.fill(poss, p);
		QualifiedTypeReference qualifiedType = new QualifiedTypeReference(annotationTypeFqn, poss);
		EclipseHandlerUtil.setGeneratedBy(qualifiedType, source);
		Annotation ann;
		if (args != null && args.length == 1 && args[0] instanceof Expression) {
			SingleMemberAnnotation sma = new SingleMemberAnnotation(qualifiedType, pS);
			sma.declarationSourceEnd = pE;
			args[0].sourceStart = pS;
			args[0].sourceEnd = pE;
			sma.memberValue = (Expression) args[0];
			EclipseHandlerUtil.setGeneratedBy(sma.memberValue, source);
			ann = sma;
		} else if (args != null && args.length >= 1) {
			NormalAnnotation na = new NormalAnnotation(qualifiedType, pS);
			na.declarationSourceEnd = pE;
			na.memberValuePairs = new MemberValuePair[args.length];
			for (int i = 0; i < args.length; i++) {
				args[i].sourceStart = pS;
				args[i].sourceEnd = pE;
				na.memberValuePairs[i] = (MemberValuePair) args[i];			
			}
			EclipseHandlerUtil.setGeneratedBy(na.memberValuePairs[0], source);
			EclipseHandlerUtil.setGeneratedBy(na.memberValuePairs[0].value, source);
			na.memberValuePairs[0].value.sourceStart = pS;
			na.memberValuePairs[0].value.sourceEnd = pE;
			ann = na;
		} else {
			MarkerAnnotation ma = new MarkerAnnotation(qualifiedType, pS);
			ma.declarationSourceEnd = pE;
			ann = ma;
		}
		EclipseHandlerUtil.setGeneratedBy(ann, source);
		if (originalAnnotationArray == null) return new Annotation[] { ann };
		Annotation[] newAnnotationArray = new Annotation[originalAnnotationArray.length + 1];
		System.arraycopy(originalAnnotationArray, 0, newAnnotationArray, 0, originalAnnotationArray.length);
		newAnnotationArray[originalAnnotationArray.length] = ann;
		node.add(ann, Kind.ANNOTATION);
		return newAnnotationArray;
	}
}
