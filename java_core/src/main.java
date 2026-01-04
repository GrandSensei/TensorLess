import java.util.List;
public class main {



    public static void main(String[] args) throws Exception {
        // 1. LOAD DATA
        String path = "digit-recognizer/train.csv";
        System.out.println("Loading data...");

        float[][] data = ExcelParse.loadData(path, 40000);

        System.out.println("Data loaded. Rows: " + data.length);
        System.out.println("Data loaded. Columns: " + data[0].length);

        //2. TEST
        System.out.println("Starting Neural Network...");
        System.out.println("Starting Testing...");
        NeuralEngine testEngine = NeuralEngine.loadModel("model_1.bin");
        testEngine.setTrainingData(data);
        testEngine.test(data);
    }

    public static void createModel() throws Exception {
        // 1. LOAD DATA
        String path = "digit-recognizer/train.csv";


        float[][] trainData = new float[20000][];
        float[][] data = ExcelParse.loadData(path, 40000);


        // Copy first 20000
        System.arraycopy(data, 0, trainData, 0, 20000);

        System.out.println("Starting Neural Network...");

        System.out.println("Loading data...");


        //2. INITIALIZE ENGINE
        NeuralEngine engine = new NeuralEngine();

        // 3. FEED DATA TO ENGINE
        engine.setTrainingData(trainData);
        engine.setNeuralNetwork(0);



        // 4. TRAIN
        System.out.println("Beginning Training...");
        long startTime = System.currentTimeMillis();
        //I went for 50 epochs that took about 425 seconds on my M3 MacBook Pro. CPU is drastically slow.
        engine.train(50);
        long endTime = System.currentTimeMillis();
        System.out.println("Training finished in " + (endTime - startTime) / 1000 + " seconds.");
        // 4. SAVE THE MODEL
        engine.saveModel("model.bin");

    }
}