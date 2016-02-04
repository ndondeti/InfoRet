package edu.asu.irs13;

public class DocumentSimillarity{
	public double simillarity;
	public int documentId;
	
	public DocumentSimillarity(){
		simillarity = 0;
		documentId = 0;
	}
	
	public DocumentSimillarity(double sim, int id){
		simillarity = sim;
		documentId = id;
	}
	
	private static int partition(DocumentSimillarity arr[], int left, int right)
	{
	      int i = left, j = right;
	      DocumentSimillarity tmp;
	      double pivot = arr[(left + right) / 2].simillarity;
	     
	      while (i <= j) {
	            while (arr[i].simillarity > pivot)
	                  i++;
	            while (arr[j].simillarity < pivot)
	                  j--;
	            if (i <= j) {
	                  tmp = arr[i];
	                  arr[i] = arr[j];
	                  arr[j] = tmp;
	                  i++;
	                  j--;
	            }
	      };
	     
	      return i;
	}
	 
	public static void quickSort(DocumentSimillarity arr[], int left, int right) {
	      int index = partition(arr, left, right);
	      if (left < index - 1)
	            quickSort(arr, left, index - 1);
	      if (index < right)
	            quickSort(arr, index, right);
	}
}
