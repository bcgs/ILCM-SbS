import java.awt.Color;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

/**
 * 
 * @author mctc@cin.ufpe.br
 *
 */

public class Grafico {
	
	public void gerarN(String nomeG, String[] curvas, String x, String y, 
		int[][] results, int[] numTags) {

		XYSeriesCollection dataset = new XYSeriesCollection();
		for (int i = 0; i < curvas.length; i++) {
			XYSeries serie = new XYSeries(curvas[i]);
	        for (int j = 0; j < results[i].length; j++) {
	            serie.add(numTags[j], results[i][j] );
			}
	        dataset.addSeries(serie);
		}
        
        JFreeChart grafico = ChartFactory.createXYLineChart(
            nomeG,      // chart title
            x,                      // x axis label
            y,                      // y axis label
            dataset,                  // data
            PlotOrientation.VERTICAL,
            true,                     // include legend
            true,                     // tooltips
            false                     // urls
        );
        
		XYPlot plot = grafico.getXYPlot();
		XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
		plot.setBackgroundPaint(Color.black);
		plot.setRenderer(renderer);
		OutputStream arquivo;
		try {
			arquivo = new FileOutputStream(nomeG + ".png");
			ChartUtilities.writeChartAsPNG(arquivo, grafico, 550, 400);
			arquivo.flush();
			arquivo.close();

		} catch (FileNotFoundException e) {
			
			e.printStackTrace();
		} catch (IOException e) {
			
			e.printStackTrace();
		}
	}
}
