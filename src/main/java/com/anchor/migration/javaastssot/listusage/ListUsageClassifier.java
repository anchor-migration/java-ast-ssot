package com.anchor.migration.javaastssot.listusage;

import com.anchor.migration.javaastssot.core.StableIds;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.CastExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Intra-procedural raw collection usage classifier (ADR-008 M2).
 * No caching — parse and classify on demand.
 */
public final class ListUsageClassifier {

    public List<ListUsageRecord> classifyFile(CompilationUnit cu, String relativePath) {
        List<ListUsageRecord> records = new ArrayList<>();
        String pkg = cu.getPackageDeclaration().map(p -> p.getNameAsString()).orElse("");

        for (TypeDeclaration<?> type : cu.getTypes()) {
            if (type instanceof ClassOrInterfaceDeclaration classType) {
                records.addAll(classifyType(classType, relativePath, pkg));
            }
        }
        return records;
    }

    private List<ListUsageRecord> classifyType(
            ClassOrInterfaceDeclaration type, String relativePath, String pkg) {
        String typeStableId = StableIds.javaType(pkg, type.getNameAsString());
        Map<String, SiteState> fieldSites = registerFieldSites(type, typeStableId);

        List<ListUsageRecord> records = new ArrayList<>();
        for (MethodDeclaration method : type.getMethods()) {
            records.addAll(classifyMethod(typeStableId, relativePath, method, fieldSites));
        }

        for (SiteState fieldSite : fieldSites.values()) {
            if (fieldSite.touched) {
                records.add(fieldSite.toRecord(relativePath, null));
            }
        }
        return records;
    }

    private static Map<String, SiteState> registerFieldSites(
            ClassOrInterfaceDeclaration type, String typeStableId) {
        Map<String, SiteState> sites = new LinkedHashMap<>();
        for (FieldDeclaration field : type.getFields()) {
            CollectionKind kind = CollectionKind.fromType(field.getElementType()).orElse(null);
            if (kind == null) {
                continue;
            }
            for (VariableDeclarator var : field.getVariables()) {
                String fieldStableId = StableIds.javaField(typeStableId, var.getNameAsString());
                sites.put(
                        var.getNameAsString(),
                        SiteState.field(fieldStableId, var.getNameAsString(), kind));
            }
        }
        return sites;
    }

    private List<ListUsageRecord> classifyMethod(
            String typeStableId,
            String relativePath,
            MethodDeclaration method,
            Map<String, SiteState> fieldSites) {
        String parameterTypes =
                method.getParameters().stream()
                        .map(p -> p.getType().asString())
                        .collect(Collectors.joining(","));
        String methodStableId = StableIds.javaMethod(typeStableId, method.getNameAsString(), parameterTypes);

        Map<String, SiteState> localSites = new LinkedHashMap<>();
        Map<String, String> localTypes = new LinkedHashMap<>();

        for (Parameter param : method.getParameters()) {
            localTypes.put(param.getNameAsString(), param.getType().asString());
            CollectionKind kind = CollectionKind.fromType(param.getType()).orElse(null);
            if (kind != null) {
                registerLocalSite(localSites, methodStableId, param.getNameAsString(), kind);
            }
        }

        method.accept(
                new VoidVisitorAdapter<Void>() {
                    @Override
                    public void visit(ObjectCreationExpr n, Void arg) {
                        CollectionKind kind = CollectionKind.fromType(n.getType()).orElse(null);
                        if (kind != null) {
                            n.getParentNode()
                                    .filter(VariableDeclarator.class::isInstance)
                                    .map(VariableDeclarator.class::cast)
                                    .ifPresent(
                                            var ->
                                                    registerLocalSite(
                                                            localSites,
                                                            methodStableId,
                                                            var.getNameAsString(),
                                                            kind));
                        }
                        super.visit(n, arg);
                    }

                    @Override
                    public void visit(VariableDeclarator n, Void arg) {
                        localTypes.put(n.getNameAsString(), n.getType().asString());
                        CollectionKind kind = CollectionKind.fromType(n.getType()).orElse(null);
                        if (kind != null) {
                            registerLocalSite(localSites, methodStableId, n.getNameAsString(), kind);
                        }
                        super.visit(n, arg);
                    }

                    @Override
                    public void visit(MethodCallExpr n, Void arg) {
                        String name = n.getNameAsString();
                        if ("add".equals(name) && n.getArguments().size() == 1) {
                            n.getScope()
                                    .ifPresent(
                                            scope ->
                                                    resolveSite(scope, localSites, fieldSites)
                                                            .ifPresent(
                                                                    site ->
                                                                            site.addEvidence(
                                                                                    "add",
                                                                                    inferType(
                                                                                            n.getArguments().get(0),
                                                                                            localTypes),
                                                                                    lineOf(n))));
                        } else if ("get".equals(name) && !n.getArguments().isEmpty()) {
                            n.getParentNode()
                                    .filter(CastExpr.class::isInstance)
                                    .map(CastExpr.class::cast)
                                    .ifPresent(
                                            cast ->
                                                    n.getScope()
                                                            .ifPresent(
                                                                    scope ->
                                                                            resolveSite(
                                                                                            scope,
                                                                                            localSites,
                                                                                            fieldSites)
                                                                                    .ifPresent(
                                                                                            site ->
                                                                                                    site.addEvidence(
                                                                                                            "cast_get",
                                                                                                            cast.getType()
                                                                                                                    .asString(),
                                                                                                            lineOf(n)))));
                        }
                        super.visit(n, arg);
                    }
                },
                null);

        List<ListUsageRecord> records = new ArrayList<>();
        for (SiteState local : localSites.values()) {
            if (local.touched) {
                records.add(local.toRecord(relativePath, methodStableId));
            }
        }
        return records;
    }

    private static void registerLocalSite(
            Map<String, SiteState> localSites,
            String methodStableId,
            String name,
            CollectionKind kind) {
        localSites.putIfAbsent(name, SiteState.local(methodStableId, name, kind));
    }

    private static Optional<SiteState> resolveSite(
            Expression scope, Map<String, SiteState> localSites, Map<String, SiteState> fieldSites) {
        if (scope instanceof NameExpr nameExpr) {
            String id = nameExpr.getNameAsString();
            if (localSites.containsKey(id)) {
                return Optional.of(localSites.get(id));
            }
            if (fieldSites.containsKey(id)) {
                return Optional.of(fieldSites.get(id));
            }
        } else if (scope instanceof FieldAccessExpr fieldAccess) {
            String id = fieldAccess.getNameAsString();
            if (fieldSites.containsKey(id)) {
                return Optional.of(fieldSites.get(id));
            }
        }
        return Optional.empty();
    }

    private static String inferType(Expression expr, Map<String, String> localTypes) {
        if (expr instanceof StringLiteralExpr) {
            return "String";
        }
        if (expr instanceof ObjectCreationExpr creation) {
            return creation.getType().asString();
        }
        if (expr instanceof CastExpr cast) {
            return cast.getType().asString();
        }
        if (expr instanceof NameExpr name) {
            String declared = localTypes.get(name.getNameAsString());
            if (declared != null) {
                return declared;
            }
            return "unknown";
        }
        return "unknown";
    }

    private static int lineOf(MethodCallExpr call) {
        return call.getBegin().map(p -> p.line).orElse(0);
    }

    private static final class SiteState {
        final String siteStableId;
        final SiteKind siteKind;
        final String variableName;
        final CollectionKind collectionKind;
        final List<ElementEvidence> evidence = new ArrayList<>();
        boolean touched;

        private SiteState(
                String siteStableId,
                SiteKind siteKind,
                String variableName,
                CollectionKind collectionKind) {
            this.siteStableId = siteStableId;
            this.siteKind = siteKind;
            this.variableName = variableName;
            this.collectionKind = collectionKind;
        }

        static SiteState local(String methodStableId, String name, CollectionKind kind) {
            return new SiteState(methodStableId + "#local:" + name, SiteKind.local, name, kind);
        }

        static SiteState field(String fieldStableId, String name, CollectionKind kind) {
            return new SiteState(fieldStableId, SiteKind.field, name, kind);
        }

        void addEvidence(String source, String type, int line) {
            touched = true;
            evidence.add(new ElementEvidence(source, TypeNameNormalizer.normalize(type), line));
        }

        ListUsageRecord toRecord(String relativePath, String methodStableId) {
            Set<String> distinctTypes = new LinkedHashSet<>();
            for (ElementEvidence item : evidence) {
                if (!"unknown".equals(item.type())) {
                    distinctTypes.add(item.type());
                }
            }
            UsageClass usageClass = TypeNameNormalizer.classifyTypes(distinctTypes);
            Confidence confidence =
                    usageClass == UsageClass.unknown
                            ? Confidence.heuristic
                            : distinctTypes.isEmpty() ? Confidence.heuristic : Confidence.high;
            return new ListUsageRecord(
                    relativePath,
                    siteStableId,
                    siteKind,
                    methodStableId,
                    variableName,
                    collectionKind,
                    usageClass,
                    List.copyOf(distinctTypes),
                    confidence,
                    List.copyOf(evidence));
        }
    }
}
