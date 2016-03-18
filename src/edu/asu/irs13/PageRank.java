package edu.asu.irs13;

import org.apache.lucene.index.*;
import org.apache.lucene.store.*;

import javafx.scene.control.ProgressBarBuilder;

import java.io.File;
import java.util.*;
import java.util.Map.Entry;

public class PageRank {

	public static HashMap<String, Float> idf = new HashMap<String, Float>();

	public static void main(String[] args) throws Exception {

		IndexReader r = IndexReader.open(FSDirectory.open(new File("index")));
		int maximumDocs = r.maxDoc();
		LinkAnalysis.numDocs = maximumDocs;
		LinkAnalysis linkAnalysis = new LinkAnalysis();
		Date d = new Date();
		System.out.println(d.getTime());
		double[] pageRank = pageRank(linkAnalysis);
		d = new Date();
		System.out.println(d.getTime());
		//int maxPage = maxPageRank(pageRank);
		//String max_page_url = r.document(maxPage).getFieldable("path").stringValue()
		//		.replace("%%", "/");
		//System.out.println(max_page_url);
		double w = 0.4;
		
		ArrayList<DocumentSimilarity> restrictedDoc = new ArrayList<DocumentSimilarity>();

		// To compute the 2-Norm of all documents
		double[] normOfDocs = normOfDoc(r);

		Scanner sc = new Scanner(System.in);
		String str = "";
		System.out.print("query> ");
		while (!(str = sc.nextLine()).equals("quit")) {

			// System.out.println(d.getTime());
			String[] terms = str.split("\\s+");

			DocumentSimilarity[] documentSimilarity = new DocumentSimilarity[maximumDocs];
			for (int i = 0; i < maximumDocs; i++) {
				documentSimilarity[i] = new DocumentSimilarity();
			}

			HashMap<String, Integer> query = freqOfQuery(terms);
			Iterator<Entry<String, Integer>> it = query.entrySet().iterator();
			int value;
			double normOfQuery = 0;
			while (it.hasNext()) {
				Map.Entry<String, Integer> pair = (Map.Entry<String, Integer>) it.next();
				value = (Integer) pair.getValue();
				normOfQuery += value * value;
			}
			// The 2-Norm of the query string
			normOfQuery = Math.sqrt(normOfQuery);

			// Computing the cosine similarity between the document and the
			// query
			for (String word : terms) {
				Term term = new Term("contents", word);
				TermDocs tdocs = r.termDocs(term);
				float idfValue = idf.get(word);
				while (tdocs.next()) {
					documentSimilarity[tdocs.doc()].simillarity += query.get(word) * tdocs.freq() * idfValue;
					documentSimilarity[tdocs.doc()].documentId = tdocs.doc();
				}
			}

			for (int i = 0; i < maximumDocs; i++) {
				if (documentSimilarity[i].simillarity != 0) {
					documentSimilarity[i].simillarity = (documentSimilarity[i].simillarity)
							/ (normOfDocs[i] * normOfQuery);
					documentSimilarity[i].simillarity = w*(pageRank[i]) + (1-w)*documentSimilarity[i].simillarity;
					restrictedDoc.add(documentSimilarity[i]);
				}
			}

			// Ranking the documents
			DocumentSimilarity.quickSort(restrictedDoc, 0, restrictedDoc.size() - 1);
			ArrayList<Integer> authoritiesAndHubsDoc = new ArrayList<Integer>();
			d = new Date();
			System.out.println(d.getTime());
			for (int i = 0; i < 10; i++) {
				String d_url = r.document(restrictedDoc.get(i).documentId).getFieldable("path").stringValue()
						.replace("%%", "/");
				System.out.println("[" + restrictedDoc.get(i).documentId + "] " + d_url);
				authoritiesAndHubsDoc.add(restrictedDoc.get(i).documentId);
			}

			System.out.print("query> ");

		}
		sc.close();
	}
	public static int maxPageRank(double[] pageRank){
		int maxIndex = 0;
		for(int i = 0; i < pageRank.length; i++){
			if(pageRank[i] >= pageRank[maxIndex]){
				maxIndex = i;
			}
		}
		System.out.println(maxIndex);
		return maxIndex;
	}
	
	private static double[] normOfDoc(IndexReader r) throws Exception {

		double[] normOfDoc = new double[r.maxDoc()];

		TermEnum t = r.terms();
		Term termsInDoc;
		TermDocs termDocs;
		float maxDoc = r.maxDoc();
		float idfVlaue = 0;
		while (t.next()) {
			idfVlaue = maxDoc / t.docFreq();
			idf.put(t.term().text(), (idfVlaue));
			termsInDoc = t.term();
			termDocs = r.termDocs(termsInDoc);
			while (termDocs.next()) {
				normOfDoc[termDocs.doc()] += termDocs.freq() * termDocs.freq();
			}
		}

		for (int i = 0; i < maxDoc; i++) {
			normOfDoc[i] = Math.sqrt(normOfDoc[i]);
		}
		return normOfDoc;

	}

	private static HashMap<String, Integer> freqOfQuery(String[] terms) {
		HashMap<String, Integer> query = new HashMap<String, Integer>();
		Integer j = 0;

		for (String ter : terms) {
			j = query.get(ter);
			if (j == null) {
				query.put(ter, 1);
			} else {
				query.replace(ter, ++j);
			}
		}
		return query;
	}

	private static double[] pageRank(LinkAnalysis linkAnalysis) {

		double[] pageRank = new double[LinkAnalysis.numDocs];
		double[] probailities = new double[LinkAnalysis.numDocs];
		double k = 1/((double)LinkAnalysis.numDocs);//0.00003991378622 //For some reason 1/LinkAnalysis.numDocs;
		float c = (float) 0;
		int convergenceIteration = 0;
		Arrays.fill(pageRank, k);

		for (int i = 0; i < LinkAnalysis.numDocs; i++) {
			probailities[i] = linkAnalysis.getLinks(i).length;
		}
		
		for (int iteration = 0; iteration < 10; iteration++) {
			double[] newPageRank = new double[LinkAnalysis.numDocs];
			double normalizer = 1;
			for (int i = 0; i < LinkAnalysis.numDocs; i++) {
				double[] mstar = new double[LinkAnalysis.numDocs];

				int[] citations = linkAnalysis.getCitations(i);
				if (citations == null || citations.length == 0) {
					Arrays.fill(mstar, k);
				} else {
					for (int citation : citations) {
						mstar[citation] = c * (1 / probailities[citation]) + (1 - c) * k;
					}
				}
				double rankOfDoc = 0;
				for (int j = 0; j < LinkAnalysis.numDocs; j++) {
					rankOfDoc += mstar[j] * pageRank[j];
				}
				newPageRank[i] = rankOfDoc;
				normalizer += rankOfDoc;
			}
			for(int i = 0; i < LinkAnalysis.numDocs; i++){
				newPageRank[i] = newPageRank[i]/normalizer;
			}
			if(Arrays.equals(pageRank, newPageRank)){
				convergenceIteration = iteration;
				break;
			}
			pageRank = newPageRank.clone();
		}
		return pageRank;
	}
}