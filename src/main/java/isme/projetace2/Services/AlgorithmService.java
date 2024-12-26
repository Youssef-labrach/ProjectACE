package isme.projetace2.Services;

import isme.projetace2.Models.Algorithm;
import isme.projetace2.Models.Project;
import isme.projetace2.Repos.AlgorithmRepo;
import org.eclipse.jdt.core.dom.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Service
public class AlgorithmService {
    @Autowired
    private AlgorithmRepo algorithmRepository;
    public List<String> detectAlgorithms(String projectPath, Project project) {
        List<String> algorithms = new ArrayList<>();

        try {
            Files.walk(Paths.get(projectPath))
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .forEach(filePath -> {
                        String code = readFile(filePath.toString());
                        if (code != null) {
                            List<String> detectedAlgorithms = analyzeFile(code, project);
                            algorithms.addAll(detectedAlgorithms);
                        }
                    });
        } catch (IOException e) {
            e.printStackTrace();
        }

        return algorithms;
    }

    private String readFile(String filePath) {
        try {
            return new String(Files.readAllBytes(Paths.get(filePath)));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private List<String> analyzeFile(String code, Project project) {
        List<String> detectedAlgorithms = new ArrayList<>();

        ASTParser parser = ASTParser.newParser(AST.JLS8);
        parser.setSource(code.toCharArray());
        parser.setKind(ASTParser.K_COMPILATION_UNIT);

        CompilationUnit cu = (CompilationUnit) parser.createAST(null);

        cu.accept(new ASTVisitor() {
            @Override
            public boolean visit(MethodDeclaration method) {
                String methodName = method.getName().toString();
                if (methodName.equals("partition")) {
                    return false; // Skip the partition method
                }

                String methodCode = getMethodCode(method, code);

                // Detect patterns in the method
                if (isSortingAlgorithm(method)) {
                    String algorithmName = "Sorting Algorithm: " + getSortingAlgorithmType(method);
                    detectedAlgorithms.add(algorithmName + "\n" + methodCode);
                    saveAlgorithm(algorithmName, methodCode, project);
                } else if (isSearchingAlgorithm(method)) {
                    String algorithmName = "Searching Algorithm: " + getSearchingAlgorithmType(method);
                    detectedAlgorithms.add(algorithmName + "\n" + methodCode);
                    saveAlgorithm(algorithmName, methodCode, project);
                }

                return super.visit(method);
            }
        });

        return detectedAlgorithms;
    }

    private void saveAlgorithm(String name, String code, Project project) {
        Algorithm algorithm = new Algorithm();
        algorithm.setName(name);
        algorithm.setCode(code);
        algorithm.setProject(project);
        algorithmRepository.save(algorithm);
    }

    private boolean isSortingAlgorithm(MethodDeclaration method) {
        return isInsertionSort(method) || isSelectionSort(method) || isQuickSort(method);
    }

    private boolean isSearchingAlgorithm(MethodDeclaration method) {
        return isBinarySearch(method) || isLinearSearch(method);
    }

    private String getSortingAlgorithmType(MethodDeclaration method) {
        if (isInsertionSort(method)) return "Insertion Sort";
        if (isSelectionSort(method)) return "Selection Sort";
        if (isQuickSort(method)) return "Quick Sort";
        return "Unknown Sorting Algorithm";
    }

    private String getSearchingAlgorithmType(MethodDeclaration method) {
        if (isBinarySearch(method)) return "Binary Search";
        if (isLinearSearch(method)) return "Linear Search";
        return "Unknown Searching Algorithm";
    }

    private boolean isQuickSort(MethodDeclaration method) {
        final boolean[] isQuickSort = {false};

        method.accept(new ASTVisitor() {
            @Override
            public boolean visit(MethodInvocation methodInvocation) {
                if (methodInvocation.getName().toString().equals("quickSort")) {
                    isQuickSort[0] = true;
                }
                return super.visit(methodInvocation);
            }

            @Override
            public boolean visit(IfStatement ifStatement) {
                if (containsPartitionCall(ifStatement) && containsRecursiveCalls(ifStatement)) {
                    isQuickSort[0] = true;
                }
                return super.visit(ifStatement);
            }
        });

        return isQuickSort[0];
    }

    private boolean containsPartitionCall(IfStatement ifStatement) {
        final boolean[] found = {false};

        ifStatement.accept(new ASTVisitor() {
            @Override
            public boolean visit(MethodInvocation methodInvocation) {
                if (methodInvocation.getName().toString().equals("partition")) {
                    found[0] = true;
                }
                return super.visit(methodInvocation);
            }
        });

        return found[0];
    }

    private boolean containsRecursiveCalls(IfStatement ifStatement) {
        final boolean[] found = {false};

        ifStatement.accept(new ASTVisitor() {
            @Override
            public boolean visit(MethodInvocation methodInvocation) {
                if (methodInvocation.getName().toString().equals("quickSort")) {
                    found[0] = true;
                }
                return super.visit(methodInvocation);
            }
        });

        return found[0];
    }

    private boolean containsNestedForLoop(ForStatement forStatement) {
        final boolean[] found = {false};

        forStatement.accept(new ASTVisitor() {
            @Override
            public boolean visit(ForStatement nestedFor) {
                found[0] = true;
                return false; // Stop further traversal
            }
        });

        return found[0];
    }

    private boolean isInsertionSort(MethodDeclaration method) {
        final boolean[] isInsertionSort = {false};

        method.accept(new ASTVisitor() {
            @Override
            public boolean visit(ForStatement forStatement) {
                if (containsInnerWhileLoop(forStatement) && containsShiftOperation(forStatement)) {
                    isInsertionSort[0] = true;
                }
                return super.visit(forStatement);
            }
        });

        return isInsertionSort[0];
    }

    private boolean isSelectionSort(MethodDeclaration method) {
        final boolean[] isSelectionSort = {false};

        method.accept(new ASTVisitor() {
            @Override
            public boolean visit(ForStatement forStatement) {
                if (containsNestedForLoop(forStatement) && containsMinIndexUpdate(forStatement)) {
                    isSelectionSort[0] = true;
                }
                return super.visit(forStatement);
            }
        });

        return isSelectionSort[0];
    }

    private boolean containsInnerWhileLoop(ForStatement forStatement) {
        final boolean[] found = {false};

        forStatement.accept(new ASTVisitor() {
            @Override
            public boolean visit(WhileStatement whileStatement) {
                found[0] = true;
                return false; // Stop further traversal
            }
        });

        return found[0];
    }

    private boolean containsShiftOperation(ForStatement forStatement) {
        final boolean[] found = {false};

        forStatement.accept(new ASTVisitor() {
            @Override
            public boolean visit(Assignment assignment) {
                if (assignment.getLeftHandSide().toString().matches(".*\\[.*\\]") &&
                        assignment.getRightHandSide().toString().matches(".*\\[.*\\]")) {
                    found[0] = true;
                }
                return super.visit(assignment);
            }
        });

        return found[0];
    }

    private boolean containsMinIndexUpdate(ForStatement forStatement) {
        final boolean[] found = {false};

        forStatement.accept(new ASTVisitor() {
            @Override
            public boolean visit(Assignment assignment) {
                if (assignment.getLeftHandSide().toString().contains("minIndex")) {
                    found[0] = true;
                }
                return super.visit(assignment);
            }
        });

        return found[0];
    }

    private boolean isBinarySearch(MethodDeclaration method) {
        final boolean[] isBinarySearch = {false};

        method.accept(new ASTVisitor() {
            @Override
            public boolean visit(WhileStatement whileStatement) {
                if (containsBinarySearchPattern(whileStatement)) {
                    isBinarySearch[0] = true;
                }
                return super.visit(whileStatement);
            }
        });

        return isBinarySearch[0];
    }

    private boolean containsBinarySearchPattern(WhileStatement whileStatement) {
        final boolean[] found = {false};

        whileStatement.accept(new ASTVisitor() {
            @Override
            public boolean visit(InfixExpression infixExpression) {
                if (infixExpression.toString().matches(".*left.*<=.*right.*") ||
                        infixExpression.toString().matches(".*array\\[.*\\].*==.*target.*")) {
                    found[0] = true;
                }
                return super.visit(infixExpression);
            }
        });

        return found[0];
    }

    private boolean isLinearSearch(MethodDeclaration method) {
        final boolean[] isLinearSearch = {false};

        method.accept(new ASTVisitor() {
            @Override
            public boolean visit(ForStatement forStatement) {
                if (containsArrayAccess(forStatement)) {
                    isLinearSearch[0] = true;
                }
                return super.visit(forStatement);
            }
        });

        return isLinearSearch[0];
    }

    private boolean containsArrayAccess(ASTNode node) {
        final boolean[] found = {false};

        node.accept(new ASTVisitor() {
            @Override
            public boolean visit(ArrayAccess arrayAccess) {
                found[0] = true;
                return false; // Stop further traversal
            }

            @Override
            public boolean visit(FieldAccess fieldAccess) {
                if (fieldAccess.toString().matches(".*\\[.*\\]")) { // Check for array-like field access
                    found[0] = true;
                }
                return super.visit(fieldAccess);
            }
        });

        return found[0];
    }

    private String getMethodCode(MethodDeclaration method, String fullCode) {
        int start = method.getStartPosition();
        int length = method.getLength();
        return fullCode.substring(start, start + length);
    }
}