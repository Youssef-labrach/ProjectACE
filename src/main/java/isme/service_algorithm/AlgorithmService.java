package isme.service_algorithm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.HttpURLConnection;
import java.nio.file.Path;
import java.util.List;
import org.eclipse.jdt.core.dom.*;
import org.springframework.web.multipart.MultipartFile;


import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.Comparator;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
@Service
public class AlgorithmService {

    @Autowired
    private AlgorithmRepo algorithmRepository;

    @Value("${service.project.url}")
    private String projectServiceUrl;

    @Autowired
    private RestTemplate restTemplate;

    public List<String> detectAlgorithms(MultipartFile projectFile, Long projectId) throws IOException {
        if (!projectExists(projectId)) {
            throw new IllegalArgumentException("Project with ID " + projectId + " does not exist.");
        }

        List<String> algorithms = new ArrayList<>();
        Path tempDir = Files.createTempDirectory("project");

        try (ZipInputStream zis = new ZipInputStream(projectFile.getInputStream())) {
            ZipEntry zipEntry;
            while ((zipEntry = zis.getNextEntry()) != null) {
                Path filePath = tempDir.resolve(zipEntry.getName());
                if (!zipEntry.isDirectory()) {
                    Files.createDirectories(filePath.getParent());
                    Files.copy(zis, filePath, StandardCopyOption.REPLACE_EXISTING);
                }
            }

            Files.walk(tempDir)
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .forEach(filePath -> {
                        String code = readFile(filePath.toString());
                        if (code != null) {
                            List<String> detectedAlgorithms = analyzeFile(code, projectId);
                            algorithms.addAll(detectedAlgorithms);
                        }
                    });
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            // Clean up temporary directory
            Files.walk(tempDir)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }

        return algorithms;
    }
//    public List<String> detectAlgorithmsFromGitHub(String githubUrl, Long projectId) throws IOException {
//        Logger logger = LoggerFactory.getLogger(AlgorithmService.class);
//
//        if (!projectExists(projectId)) {
//            throw new IllegalArgumentException("Project with ID " + projectId + " does not exist.");
//        }
//
//        List<String> algorithms = new ArrayList<>();
//        Path tempDir = Files.createTempDirectory("project");
//
//        logger.info("Downloading project from GitHub URL: {}", githubUrl);
//
//        // Download the project ZIP from GitHub
//        URL url = new URL(githubUrl);
//        try (ZipInputStream zis = new ZipInputStream(url.openStream())) {
//            ZipEntry zipEntry;
//            while ((zipEntry = zis.getNextEntry()) != null) {
//                Path filePath = tempDir.resolve(zipEntry.getName());
//                if (!zipEntry.isDirectory()) {
//                    Files.createDirectories(filePath.getParent());
//                    Files.copy(zis, filePath, StandardCopyOption.REPLACE_EXISTING);
//                    logger.info("Extracted file: {}", filePath);
//                }
//            }
//
//            Files.walk(tempDir)
//                    .filter(Files::isRegularFile)
//                    .filter(path -> path.toString().endsWith(".java"))
//                    .forEach(filePath -> {
//                        String code = readFile(filePath.toString());
//                        if (code != null) {
//                            List<String> detectedAlgorithms = analyzeFile(code, projectId);
//                            algorithms.addAll(detectedAlgorithms);
//                            logger.info("Detected algorithms in file {}: {}", filePath, detectedAlgorithms);
//                        }
//                    });
//        } catch (IOException e) {
//            logger.error("Error processing GitHub project", e);
//        } finally {
//            // Clean up temporary directory
//            Files.walk(tempDir)
//                    .sorted(Comparator.reverseOrder())
//                    .map(Path::toFile)
//                    .forEach(File::delete);
//        }
//
//        logger.info("Total algorithms detected: {}", algorithms.size());
//        return algorithms;
//    }
public String getGitHubZipUrl(String githubUrl) {
    if (githubUrl == null || githubUrl.isEmpty()) {
        throw new IllegalArgumentException("GitHub URL cannot be null or empty");
    }

    if (githubUrl.endsWith(".git")) {
        githubUrl = githubUrl.substring(0, githubUrl.length() - 4);
    }

    // Tentez "main" puis "master" si nécessaire
    String zipUrl = githubUrl + "/archive/refs/heads/main.zip";
    try {
        HttpURLConnection connection = (HttpURLConnection) new URL(zipUrl).openConnection();
        connection.setRequestMethod("HEAD");
        if (connection.getResponseCode() == 404) {
            zipUrl = githubUrl + "/archive/refs/heads/master.zip";
        }
    } catch (IOException e) {
        throw new RuntimeException("Failed to validate GitHub ZIP URL", e);
    }

    return zipUrl;
}
    public List<String> detectAlgorithmsFromGitHub(String githubUrl, Long projectId) throws IOException {
        Logger logger = LoggerFactory.getLogger(AlgorithmService.class);

        if (!projectExists(projectId)) {
            throw new IllegalArgumentException("Project with ID " + projectId + " does not exist.");
        }

        // Transformer l'URL GitHub
        String zipUrl = getGitHubZipUrl(githubUrl);
        logger.info("Transformed GitHub URL to ZIP: {}", zipUrl);

        List<String> algorithms = new ArrayList<>();
        Path tempDir = Files.createTempDirectory("project");
        logger.info("Temporary directory created at: {}", tempDir.toAbsolutePath());

        try (ZipInputStream zis = new ZipInputStream(new URL(zipUrl).openStream())) {
            ZipEntry zipEntry;
            while ((zipEntry = zis.getNextEntry()) != null) {
                Path filePath = tempDir.resolve(zipEntry.getName());
                if (!zipEntry.isDirectory() && zipEntry.getName().endsWith(".java")) {
                    Files.createDirectories(filePath.getParent());
                    Files.copy(zis, filePath, StandardCopyOption.REPLACE_EXISTING);
                    logger.info("Extracted Java file: {}", filePath);
                }
            }

            // Analyse des fichiers Java
            Files.walk(tempDir)
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .forEach(filePath -> {
                        String code = readFile(filePath.toString());
                        if (code != null) {
                            List<String> detectedAlgorithms = analyzeFile(code, projectId);
                            algorithms.addAll(detectedAlgorithms);
                            logger.info("Detected algorithms in file {}: {}", filePath, detectedAlgorithms);
                        }
                    });
        } catch (IOException e) {
            logger.error("Error processing GitHub project", e);
        } finally {
            // Nettoyage du répertoire temporaire
            try {
                Files.walk(tempDir)
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            } catch (IOException e) {
                logger.error("Error cleaning up temporary directory", e);
            }
        }

        logger.info("Total algorithms detected: {}", algorithms.size());
        return algorithms;
    }

    private boolean projectExists(Long projectId) {
        String url = projectServiceUrl + "/" + projectId;
        try {
            restTemplate.getForObject(url, Void.class);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    private String getRecommendation(String algorithmName) {
        // Logic to provide a recommendation based on the algorithm name
        switch (algorithmName) {
            case "Sorting Algorithm: Insertion Sort":
                return "Consider using QuickSort for better average-case complexity.";
            case "Sorting Algorithm: Selection Sort":
                return "Consider using QuicSort for better performance on large datasets.";
            case "Sorting Algorithm: Quick Sort", "Searching Algorithm: Binary Search":
                return "You are using the best algorithm.";
            case "Searching Algorithm: Linear Search":
                return "Consider using Binary Search for better performance.";
            default:
                return "No recommendation available for this algorithm.";
        }
    }
    private String readFile(String filePath) {
        try {
            return new String(Files.readAllBytes(Paths.get(filePath)));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private List<String> analyzeFile(String code, Long projectId) {
        Logger logger = LoggerFactory.getLogger(AlgorithmService.class);
        List<String> detectedAlgorithms = new ArrayList<>();

        ASTParser parser = ASTParser.newParser(AST.JLS8);
        parser.setSource(code.toCharArray());
        parser.setKind(ASTParser.K_COMPILATION_UNIT);

        CompilationUnit cu = (CompilationUnit) parser.createAST(null);

        cu.accept(new ASTVisitor() {
            @Override
            public boolean visit(MethodDeclaration method) {
                String methodName = method.getName().toString();
                logger.info("Analyzing method: {}", methodName);
                if (methodName.equals("partition")) {
                    return false; // Skip the partition method
                }

                String methodCode = getMethodCode(method, code);

                // Detect patterns in the method
                if (isSortingAlgorithm(method)) {
                    String algorithmName = "Sorting Algorithm: " + getSortingAlgorithmType(method);
                    String recommendation = getRecommendation(algorithmName);
                    detectedAlgorithms.add(algorithmName + "\n" + methodCode);
                    saveAlgorithm(algorithmName, methodCode, projectId, recommendation);
                    logger.info("Detected sorting algorithm: {}", algorithmName);
                } else if (isSearchingAlgorithm(method)) {
                    String algorithmName = "Searching Algorithm: " + getSearchingAlgorithmType(method);
                    String recommendation = getRecommendation(algorithmName);
                    detectedAlgorithms.add(algorithmName + "\n" + methodCode);
                    saveAlgorithm(algorithmName, methodCode, projectId, recommendation);
                    logger.info("Detected searching algorithm: {}", algorithmName);
                }

                return super.visit(method);
            }
        });

        logger.info("Algorithms detected in file: {}", detectedAlgorithms);
        return detectedAlgorithms;
    }

    private void saveAlgorithm(String name, String code, Long projectId, String recommendation) {
        Algorithm algorithm = new Algorithm();
        algorithm.setName(name);
        algorithm.setCode(code);
        algorithm.setProjectId(projectId);
        algorithm.setRecommendation(recommendation);
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



    public List<Algorithm> getAlgorithmsByProjectId(Long projectId) {
        return algorithmRepository.findByProjectId(projectId);
    }



}

