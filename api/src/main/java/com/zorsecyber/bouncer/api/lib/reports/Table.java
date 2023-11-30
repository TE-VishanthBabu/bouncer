package com.zorsecyber.bouncer.api.lib.reports;

import java.util.ArrayList;

public class Table {
	public long id;
	public int columns;
	private String[] headers;
	private ArrayList<String[]> rows;
	
	public Table(int columns)
	{
		headers = new String[columns];
		rows = new ArrayList<String[]>();
	}

	public void setHeaders(String[] headers) throws Exception {
		this.verifyRowLength(headers);
		this.headers = headers;
	}
	
	public void addRow(String [] row) throws Exception
	{
		this.verifyRowLength(row);
		rows.add(row);
	}
	
	private boolean verifyRowLength(String[] row) throws Exception
	{
		if (row.length == columns)
			return true;
		throw new Exception("Incorrect array size should be "+columns);
	}
	
	public String toString()
	{
		for(String s : headers)
		{
			System.out.print(s+"\t");
			System.out.println();
		}
		int i = 0;
		while (i<10 && i < rows.size())
		{
			for (String s : rows.get(i))
			{
				System.out.print(s+"\t");
				System.out.println();
			}
		}
		return null;
	}

}
