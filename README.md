# Lombok meta annotations
Draft for meta annotations in lombok

This lombok handler adds support for meta annotations in Lombok. This function was implemented as an extension to be independent of the current Lombok version and to emphasize the experimental status. Furthermore the project can be used as a template for other external extensions which might be useful for some people.

It is based on an idea by [@Maaartinus](https://github.com/rzwitserloot/lombok/issues/557#issuecomment-317985839).

## Setup

### Javac
- Download the latest release
- Add the `jar` to your classpath 

### Eclipse
- Download the latest release
- Copy the `jar` into your eclipse directory
- Add `-Xbootclasspath/a:metaannotations-X.X.X.jar` to your `eclipse.ini`

## Usage

This feature uses the lombok configuration system key `lombok.metaAnnotations`. To register an annotation as meta annotation add lines using the syntax described below to your `lombok.config`.

```
lombok.metaAnnotations += @<meta annotation> <fully.qualified.name.annotation>(<key>=<value>,...) ...
```

**Rules**
- Spaces are only allowed as separator between annotations
- Every target annotation must be fully qualified
- The meta annotation must be the simple name only
- The target annotation does not have to be part of lombok
- The meta annotation is preserved

### Examples

```java
lombok.metaAnnotations += @NoEtters lombok.Getter(lombok.AccessLevel.NONE) lombok.Setter(lombok.AccessLevel.NONE)
```

```java
lombok.metaAnnotations += @DTO lombok.AllArgsConstructor lombok.Data lombok.Builder
```

```java
lombok.metaAnnotations += @Annotation @java.lang.annotation.Target({java.lang.annotation.ElementType.FIELD,java.lang.annotation.ElementType.TYPE}) @java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.SOURCE)
```



## Missing features
- Copy values of a meta annotation to the target annotation, including default values. 
- Package support, right now it does not check the actual type of the meta annotation
- A way to remove the original meta annotation
- Using a meta annotation as a target annotation
- Stable parser instead of the string split stuff
- Proper error handling
