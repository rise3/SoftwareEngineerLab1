import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class lab1 {
    private static Map<String, Map<String, Integer>> graph = new HashMap<>();
    private static boolean stopRandomWalk = false;
    private static Thread randomWalkThread;
    private static JTextArea resultArea; // 将resultArea定义为类字段

    public static void main(String[] args) {
        SwingUtilities.invokeLater(lab1::createAndShowGUI);
    }

    private static void createAndShowGUI() {
        JFrame frame = new JFrame("Text Graph Application");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JLabel filePathLabel = new JLabel("File Path:");
        JTextField filePathField = new JTextField();
        JButton loadButton = new JButton("Load File");

        resultArea = new JTextArea(10, 50); // 初始化resultArea
        resultArea.setEditable(false);

        JComboBox<String> functionSelector = new JComboBox<>(new String[]{
                "Query Bridge Words",
                "Generate New Text",
                "Calculate Shortest Path",
                "Random Walk"
        });

        JButton executeButton = new JButton("Execute");
        JTextField inputField1 = new JTextField();
        JTextField inputField2 = new JTextField();
        JButton stopButton = new JButton("Stop Random Walk");
        stopButton.setEnabled(false);

        JButton showGraphButton = new JButton("Show Graph");

        panel.add(filePathLabel);
        panel.add(filePathField);
        panel.add(loadButton);
        panel.add(new JLabel("Select Function:"));
        panel.add(functionSelector);
        panel.add(new JLabel("Input 1:"));
        panel.add(inputField1);
        panel.add(new JLabel("Input 2:"));
        panel.add(inputField2);
        panel.add(executeButton);
        panel.add(stopButton);
        panel.add(showGraphButton);
        panel.add(new JScrollPane(resultArea));

        loadButton.addActionListener(e -> {
            String filePath = filePathField.getText();
            String textContent = readFile(filePath);
            if (textContent != null) {
                String[] words = preprocessText(textContent);
                graph = buildGraph(words);
                resultArea.setText("Graph loaded successfully.");
            } else {
                resultArea.setText("Failed to read file.");
            }
        });

        executeButton.addActionListener(e -> {
            String selectedFunction = (String) functionSelector.getSelectedItem();
            String input1 = inputField1.getText();
            String input2 = inputField2.getText();
            String result = "";

            switch (selectedFunction) {
                case "Query Bridge Words":
                    result = queryBridgeWords(input1, input2);
                    break;
                case "Generate New Text":
                    result = generateNewText(input1);
                    break;
                case "Calculate Shortest Path":
                    result = calShortestPath(input1, input2);
                    break;
                case "Random Walk":
                    stopButton.setEnabled(true);
                    stopRandomWalk = false;
                    randomWalkThread = new Thread(() -> {
                        String walkResult = randomWalk(resultArea);
                        SwingUtilities.invokeLater(() -> {
                            resultArea.setText(walkResult);
                            stopButton.setEnabled(false);
                        });
                    });
                    randomWalkThread.start();
                    break;
            }

            if (!selectedFunction.equals("Random Walk")) {
                resultArea.setText(result);
            }
        });

        stopButton.addActionListener(e -> {
            stopRandomWalk = true;
            if (randomWalkThread != null) {
                randomWalkThread.interrupt();
            }
        });

        showGraphButton.addActionListener(e -> showDirectedGraph(graph));

        frame.add(panel);
        frame.setVisible(true);
    }

    private static String readFile(String filePath) {
        StringBuilder content = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                content.append(line).append(" ");
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return content.toString();
    }

    private static String[] preprocessText(String text) {
        return text.replaceAll("[^a-zA-Z\\s]", " ")
                .toLowerCase()
                .split("\\s+");
    }

    private static Map<String, Map<String, Integer>> buildGraph(String[] words) {
        Map<String, Map<String, Integer>> graph = new HashMap<>();
        for (int i = 0; i < words.length; i++) {
            if (!words[i].isEmpty()) {
                graph.putIfAbsent(words[i], new HashMap<>());
            }
            if (i + 1 < words.length && !words[i + 1].isEmpty()) {
                Map<String, Integer> adjList = graph.get(words[i]);
                adjList.put(words[i + 1], adjList.getOrDefault(words[i + 1], 0) + 1);
            }
        }
        return graph;
    }

    private static String queryBridgeWords(String word1, String word2) {
        if (!graph.containsKey(word1) || !graph.containsKey(word2)) {
            return "No " + word1 + " or " + word2 + " in the graph!";
        }

        Map<String, Integer> adjList1 = graph.get(word1);
        boolean bridgeWordFound = false;
        StringBuilder bridgeWords = new StringBuilder();

        for (String word3 : adjList1.keySet()) {
            Map<String, Integer> adjList2 = graph.get(word3);
            if (adjList2 != null && adjList2.containsKey(word2)) {
                if (bridgeWordFound) {
                    bridgeWords.append(", ");
                }
                bridgeWords.append(word3);
                bridgeWordFound = true;
            }
        }

        if (bridgeWordFound) {
            return "The bridge words from " + word1 + " to " + word2 + " are: " + bridgeWords.toString() + ".";
        } else {
            return "No bridge words from " + word1 + " to " + word2 + "!";
        }
    }

    private static String generateNewText(String inputText) {
        String[] words = preprocessText(inputText);
        StringBuilder newSentence = new StringBuilder();
        Random random = new Random();

        for (int i = 0; i < words.length - 1; i++) {
            newSentence.append(words[i]).append(" ");
            String word1 = words[i];
            String word2 = words[i + 1];

            if (graph.containsKey(word1)) {
                List<String> bridgeWords = new ArrayList<>();
                for (String bridge : graph.get(word1).keySet()) {
                    if (graph.get(bridge).containsKey(word2)) {
                        bridgeWords.add(bridge);
                    }
                }

                if (!bridgeWords.isEmpty()) {
                    String selectedBridge = bridgeWords.get(random.nextInt(bridgeWords.size()));
                    newSentence.append(selectedBridge).append(" ");
                }
            }
        }

        newSentence.append(words[words.length - 1]);
        return newSentence.toString();
    }

    private static String calShortestPath(String word1, String word2) {
        if (!graph.containsKey(word1) || !graph.containsKey(word2)) {
            return "No " + word1 + " or " + word2 + " in the graph!";
        }

        Map<String, Integer> distances = new HashMap<>();
        Map<String, String> previousNodes = new HashMap<>();
        PriorityQueue<String> nodes = new PriorityQueue<>(Comparator.comparingInt(distances::get));

        for (String node : graph.keySet()) {
            distances.put(node, Integer.MAX_VALUE);
            previousNodes.put(node, null);
        }

        distances.put(word1, 0);
        nodes.add(word1);

        while (!nodes.isEmpty()) {
            String currentNode = nodes.poll();

            if (currentNode.equals(word2)) {
                break;
            }

            Map<String, Integer> neighbors = graph.get(currentNode);
            for (Map.Entry<String, Integer> neighbor : neighbors.entrySet()) {
                int newDist = distances.get(currentNode) + neighbor.getValue();
                if (newDist < distances.get(neighbor.getKey())) {
                    distances.put(neighbor.getKey(), newDist);
                    previousNodes.put(neighbor.getKey(), currentNode);
                    nodes.add(neighbor.getKey());
                }
            }
        }

        if (distances.get(word2) == Integer.MAX_VALUE) {
            return "No path from " + word1 + " to " + word2 + "!";
        }

        List<String> path = new ArrayList<>();
        for (String at = word2; at != null; at = previousNodes.get(at)) {
            path.add(0, at);
        }

        StringBuilder pathStr = new StringBuilder();
        pathStr.append("The shortest path from ").append(word1).append(" to ").append(word2).append(" is: ");
        for (String node : path) {
            pathStr.append(node).append(" ");
        }
        pathStr.append("\nPath length: ").append(distances.get(word2));

        return pathStr.toString();
    }

    private static String randomWalk(JTextArea resultArea) {
        Random random = new Random();
        List<String> visitedEdges = new ArrayList<>();
        List<String> path = new ArrayList<>();

        List<String> nodes = new ArrayList<>(graph.keySet());
        String currentNode = nodes.get(random.nextInt(nodes.size()));
        path.add(currentNode);

        while (!stopRandomWalk) {
            Map<String, Integer> neighbors = graph.get(currentNode);
            if (neighbors.isEmpty()) {
                break;
            }
            List<String> nextNodes = new ArrayList<>(neighbors.keySet());
            String nextNode = nextNodes.get(random.nextInt(nextNodes.size()));
            String edge = currentNode + " -> " + nextNode;

            if (visitedEdges.contains(edge)) {
                break;
            }

            visitedEdges.add(edge);
            path.add(nextNode);
            currentNode = nextNode;


            // 输出当前游走的路径
            StringBuilder currentPath = new StringBuilder();
            for (String node : path) {
                currentPath.append(node).append(" ");
            }
            SwingUtilities.invokeLater(() -> resultArea.append("Current path: " + currentPath.toString() + "\n"));

            try {
                TimeUnit.SECONDS.sleep(3); // Wait for 10 seconds
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        StringBuilder pathStr = new StringBuilder();
        for (String node : path) {
            pathStr.append(node).append(" ");
        }

        return "Random Walk Path: " + pathStr.toString();
    }

    private static void showDirectedGraph(Map<String, Map<String, Integer>> graph) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Map<String, Integer>> entry : graph.entrySet()) {
            String word = entry.getKey();
            sb.append(word).append("\n");
            Map<String, Integer> adjList = entry.getValue();
            for (Map.Entry<String, Integer> adjEntry : adjList.entrySet()) {
                sb.append(String.format("%s -> %s [weight=%d]%n", word, adjEntry.getKey(), adjEntry.getValue()));
            }
        }
        JOptionPane.showMessageDialog(null, sb.toString(), "Directed Graph", JOptionPane.INFORMATION_MESSAGE);
    }
}
