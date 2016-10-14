package siscom;

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

	public void gerar(String nome, String x, String y, int[] result, int[] numTags) {
		XYSeries serie = new XYSeries("Curva");
        for (int i = 0; i < result.length; i++) {
               serie.add(numTags[i], result[i]);
		}
        
        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(serie);
        
        JFreeChart grafico = ChartFactory.createXYLineChart(
                nome,      // chart title
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
        plot.setRenderer(renderer);
		OutputStream arquivo;
		try {
			arquivo = new FileOutputStream(nome + ".png");
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
