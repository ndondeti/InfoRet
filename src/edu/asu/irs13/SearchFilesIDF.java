package edu.asu.irs13;

import org.apache.lucene.index.*;
import org.apache.lucene.store.*;

import java.io.File;
import java.util.*;
import java.util.Map.Entry;

public class SearchFilesIDF {

	public static HashMap<String, Float> idf = new HashMap<String, Float>();

	public static void main(String[] args) throws Exception {
		

		IndexReader r = IndexReader.open(FSDirectory.open(new File("index")));
		int maximumDocs = r.maxDoc();
		LinkAnalysis.numDocs = maximumDocs;
		LinkAnalysis linkAnalysis = new LinkAnalysis();
		Date d;
		ArrayList<DocumentSimilarity> restrictedDoc = new ArrayList<DocumentSimilarity>();

		// To compute the 2-Norm of all documents
		double[] normOfDocs = normOfDoc(r);

		Scanner sc = new Scanner(System.in);
		String str = "";
		System.out.print("query> ");
		while (!(str = sc.nextLine()).equals("quit")) {
			
			//System.out.println(d.getTime());
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
					restrictedDoc.add(documentSimilarity[i]);
				}
			}

			// Ranking the documents
			DocumentSimilarity.quickSort(restrictedDoc, 0, restrictedDoc.size() - 1);
			ArrayList<Integer> authoritiesAndHubsDoc = new ArrayList<Integer>();
			d = new Date();
			System.out.println(d.getTime());
			
			//Retreving the top 10 documents
			for (int i = 0; i < 30; i++) {
				authoritiesAndHubsDoc.add(restrictedDoc.get(i).documentId);
			}
			
			HashSet<Integer> nodes = new HashSet<Integer>();
			HashMap<Integer, Integer> index = new HashMap<>();
			
			//Finding out the nodes linked to the top 10 documents
			for (Integer doc : authoritiesAndHubsDoc) {
				int[] links = linkAnalysis.getLinks(doc);
				int[] citations = linkAnalysis.getCitations(doc);
				for (int link : links) {
					nodes.add(link);
				}
				for (int citation : citations) {
					nodes.add(citation);
				}
			}
			d = new Date();
			System.out.println(d.getTime());
			int sizeOfBaseSet = nodes.size();
			int i = 0;
			int[][] adj = new int[sizeOfBaseSet][sizeOfBaseSet];
			double[] authorities = new double[sizeOfBaseSet];
			double[] hubs = new double[sizeOfBaseSet];

			//Initially authorities value is filled with 0 and hubs value is filled with 1.
			Arrays.fill(authorities, 0);
			Arrays.fill(hubs, 1);
			
			//Storing the idex of each document
			for(Integer doc : nodes){
				index.put(doc, i);
				i++;
			}
			
			i = 0;
			
			//Constructing the adjacency matrix for the base set. While getting links and citations
			//we are ignoring all the nodes that are not part of the base set.
			for (Integer doc : nodes) {
				int[] links = linkAnalysis.getLinks(doc);
				int[] citations = linkAnalysis.getCitations(doc);
				for (int link : links) {
					if(nodes.contains(link))
						adj[index.get(doc)][index.get(link)] = 1;
				}
				for (int citation : citations) {
					if(nodes.contains(citation))
						adj[index.get(citation)][index.get(doc)] = 1;
				}
			}
			d = new Date();
			System.out.println(d.getTime());
			//Computing the hubs and authorities score over 30 iterations
			for (int k = 0; k < 30; k++) {
				double authoritiesNorm = 0;
				double hubsNorm = 0;
				for (i = 0; i < sizeOfBaseSet; i++) {
					for (int j = 0; j < sizeOfBaseSet; j++) {
						if (adj[j][i] == 1){
							authorities[i] += hubs[j];
							authoritiesNorm += authorities[i] * authorities[i];
						}
					}
				}
				for (i = 0; i < sizeOfBaseSet; i++) {
					for (int j = 0; j < sizeOfBaseSet; j++) {
						if (adj[i][j] == 1){
							hubs[i] += authorities[j];
							hubsNorm += hubs[i]*hubs[i];
						}
					}
				}
				authoritiesNorm = Math.sqrt(authoritiesNorm);
				hubsNorm = Math.sqrt(hubsNorm);
				//Normalizing all the hubs and authorities score by L2-Norm
				for(int j = 0;j < sizeOfBaseSet; j++){
					authorities[j] = authorities[j]/authoritiesNorm;
					hubs[j] = hubs[j]/hubsNorm;
				}
			}
			d = new Date();
			System.out.println(d.getTime());
			List nodesInArray = new ArrayList<Integer>(nodes);
			//Printing the top 10 documents with highest authorities and hubs score
			System.out.println("The top 10 documents with hub and authorites are:");
			for(i = 0; i < 10; i++){
				double authMax = 0;
				double hubsMax = 0;
				int authMaxIndex = 0;
				int hubMaxIndex = 0;
				for(int j =0; j < sizeOfBaseSet; j++){
					if(authorities[j] >= authMax ){
						authMax = authorities[j];
						authMaxIndex = j;
					}
					if(hubs[j] >= hubsMax ){
						hubsMax = hubs[j];
						hubMaxIndex = j;
					}
				}
				authorities[authMaxIndex] = 0;
				hubs[hubMaxIndex] = 0;
				System.out.print(nodesInArray.get(hubMaxIndex) + "\t\t" + nodesInArray.get(authMaxIndex) + "\n");
			}
			d = new Date();
			System.out.println(d.getTime());
			System.out.print("query> ");

		}
		sc.close();
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
}