package it.himyd.spark.analysis.batch;

import java.io.Serializable;

import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.streaming.api.java.JavaDStream;
import org.apache.spark.streaming.api.java.JavaPairDStream;

import it.himyd.stock.StockCluster;
import it.himyd.stock.StockOHLC;
import it.himyd.stock.finance.yahoo.Stock;
import scala.Tuple2;

public class AnalysisBatchRunner implements Serializable {

	private static final long serialVersionUID = 1L;

	/* OHLC ANALYSIS */

	public void printPriceMostUp(JavaRDD<StockOHLC> ohlc, int topK) {
		printPriceMostX(ohlc, topK, false);
	}

	public void printPriceMostDown(JavaRDD<StockOHLC> ohlc, int topK) {
		printPriceMostX(ohlc, topK, true);
	}

	public void printPriceMostX(JavaRDD<StockOHLC> ohlc, int topK, boolean down) {
		String message = "Top " + topK + " " + (down ? "Negative" : "Positive");

		JavaPairRDD<String, Double> most = ohlc.mapToPair(x -> new Tuple2<>(x.getSymbol(),
				(x.getClose() != x.getOpen() ? (((x.getClose() / x.getOpen()) - 1) * 100) : 0.0)));

		System.out.println(message);

		most.mapToPair(t -> new Tuple2<Double, String>(t._2(), t._1())).sortByKey(down).take(topK)
				.forEach(t -> System.out.println(t._2() + " | " + t._1() + "%"));
	}

	/* CLUSTER ANALYSIS */

	public void printSimilarStocks(JavaRDD<StockCluster> clusters) {

		JavaPairRDD<String, String> date2cluster = clusters
				.mapToPair(t -> new Tuple2<>(t.getClustertime() + "--" + t.getCluster(), t.getSymbol()));

		JavaPairRDD<String, String> date2pair = date2cluster.reduceByKey((v1, v2) -> v1 + "::" + v2);

		JavaPairRDD<String, Integer> stocks2one = date2pair.mapToPair(t -> new Tuple2<>(t._2(), new Integer(1)));

		JavaPairRDD<String, Integer> stocks2count = stocks2one.reduceByKey((v1, v2) -> v1 + v2);

		stocks2count.sortByKey().foreach(t -> {
			if (t._1().contains("::")) {
				System.out.println(t);
			}

		});
	}

}
