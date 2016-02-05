package edu.asu.irs13;

import org.apache.lucene.index.*;
import org.apache.lucene.store.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.Scanner;
import java.util.*;

public class SearchFiles {
	
	public static void main(String[] args) throws Exception
	{	
		IndexReader r = IndexReader.open(FSDirectory.open(new File("index")));
		int maximumDocs = r.maxDoc();
		Date d;
		
		double[] normOfDocs = normOfDoc(r);
		HashMap<String, Float> idf = computeIdf(r);
		Scanner sc = new Scanner(System.in);
		String str = "";
		System.out.print("query> ");
		while(!(str = sc.nextLine()).equals("quit"))
		{
			d = new Date();
			System.out.println(d.getTime());
			String[] terms = str.split("\\s+");
			DocumentSimillarity[] documetnSimillarity = new DocumentSimillarity[maximumDocs];
			for (int i = 0; i< maximumDocs; i++){
				documetnSimillarity[i] = new DocumentSimillarity();
			}
			HashMap<String, Integer> query = freqOfQuery(terms);
			Iterator it = query.entrySet().iterator();
			int value;
			double normOfQuery = 0;
			while (it.hasNext()) {
				Map.Entry<String, Integer> pair = (Map.Entry<String, Integer>)it.next();
				value = (Integer)pair.getValue(); 
				normOfQuery += value*value;
			}
			normOfQuery = Math.sqrt(normOfQuery);			
			
			for(String word : terms)
			{
				Term term = new Term("contents", word);
				TermDocs tdocs = r.termDocs(term);
				float idfValue = idf.get(word);
				while(tdocs.next())
				{
					documetnSimillarity[tdocs.doc()].simillarity += query.get(word) * tdocs.freq() * idfValue;
					documetnSimillarity[tdocs.doc()].documentId = tdocs.doc();					
				}
			}
			
			for(int i = 0;i < maximumDocs;i++){
				documetnSimillarity[i].simillarity = (documetnSimillarity[i].simillarity)/(normOfDocs[i] * normOfQuery);
			}
			d = new Date();
			System.out.println(d.getTime());			
			DocumentSimillarity.quickSort(documetnSimillarity, 0, documetnSimillarity.length - 1);
			d = new Date();
			System.out.println(d.getTime());
			for(int i = 0; i < 10 ;i++){
				System.out.println("["+documetnSimillarity[i].documentId+"]");
			}
			
			System.out.print("query> ");
			
		}
		sc.close();
	}
	
	private static double[] normOfDoc(IndexReader r)throws Exception{
		
		double[] normOfDoc = new double[r.maxDoc()];
		
		TermEnum t = r.terms();
		Term termsInDoc;
		TermDocs termDocs;
		while(t.next())
		{
			termsInDoc = t.term();
			termDocs = r.termDocs(termsInDoc);
			while(termDocs.next()){
				normOfDoc[termDocs.doc()] += termDocs.freq()*termDocs.freq(); 
			}
		}
		
		for(int i = 0; i < r.maxDoc(); i++){
			normOfDoc[i] = Math.sqrt(normOfDoc[i]);
		}
		return normOfDoc;

	}
	
	private static HashMap<String, Integer> freqOfQuery(String[] terms){
		HashMap<String, Integer> query = new HashMap<String, Integer>();
		Integer j = 0;
		
		for (String ter : terms){
			j = query.get(ter);
			if(j == null){
				query.put(ter, 1);
			}
			else{
				query.replace(ter, ++j);
			}
		}
		return query;
	}
	
	private static HashMap<String, Float> computeIdf(IndexReader r) throws Exception{
		TermEnum t = r.terms();
		HashMap<String, Float> idfMap =  new HashMap<String, Float>();
		String minIdfTerm = null;
		float min = 99999999;
		float maxDoc = r.maxDoc();
		float idf = 0;
		while(t.next())
		{
			idf = maxDoc/t.docFreq();
			if(min > idf){
				min = idf;
				minIdfTerm = t.term().text();
			}				
			idfMap.put(t.term().text(), (idf));
		}
		return idfMap;
	}
}
