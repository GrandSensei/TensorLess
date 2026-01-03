import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.channels.ScatteringByteChannel;
import java.util.ArrayList;
import java.util.List;

public class ExcelParse {

    //This code is refactored from a previous project of mine for some work stuff

    public static float[][] loadData(String path, int maxRows) throws Exception {
        List<float[]> data = new ArrayList<>();
        String line;

        try(BufferedReader br = new BufferedReader(new FileReader(path))){
            br.readLine();
            int count = 0;
            while((line = br.readLine()) != null){
                if(maxRows!=-1 && count>= maxRows) break;
                String[] values = line.split(",");

                float[] row = new float[values.length];
                row[0] = Float.parseFloat(values[0]);
                //convert the string to float
                for(int i=1;i<values.length;++i){
                    row[i] = Float.parseFloat(values[i])/255.f;
                }
                data.add(row);
                count++;

                if (count%1000 == 0) System.out.println("Loaded " + count + " rows");
            }

        }catch (Exception e){
            e.printStackTrace();
            throw new Exception("Error while reading the data file");
        }
        float[][] dataArray = new float[data.size()][data.get(0).length];
        return data.toArray(dataArray);
    }
}
