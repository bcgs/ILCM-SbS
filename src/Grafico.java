import java.awt.Color;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

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
	
	public void gerarEstimadores(String nomeG, String x, String y, 
		int[] result1, int[] result2, int[] result3, int[] numTags,
		String est1, String est2, String est3) {
		XYSeries serie1 = new XYSeries(est1);
		XYSeries serie2 = new XYSeries(est2);
		XYSeries serie3 = new XYSeries(est3);
        for (int i = 0; i < result1.length; i++) {
           serie1.add(numTags[i], result1[i]);
           serie2.add(numTags[i], result2[i]);
           serie3.add(numTags[i], result3[i]);

		}
        
        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(serie1);
        dataset.addSeries(serie2);
        dataset.addSeries(serie3);
        
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
	public void gerarDupla(String nomeG, String x, String y, 
		int[] result1, int[] result2, int[] numTags, 
		String est1, String est2) {
		XYSeries serie1 = new XYSeries(est1);
		XYSeries serie2 = new XYSeries(est2);
        for (int i = 0; i < result1.length; i++) {
			serie1.add(numTags[i], result1[i]);
			serie2.add(numTags[i], result2[i]);
		}
        
        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(serie1);
        dataset.addSeries(serie2);
        
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
