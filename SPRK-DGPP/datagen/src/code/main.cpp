// Created by Kaustubh Shivdikar
//
//  (C) All Rights Reserved


# include "../lib/functions.h"
# include "../lib/debugger.h"

// DEFINES
#define MAT_DIM 5

using namespace std;

// Global Variables
string file_name = "input_data.csv";
string com = ",";



/*
OUTPUT: Adjacency List
1,2,4
2,3,5,1
3,2,6
4,1,5,7
*/



// ************ MAIN FUNCTION ************


int main (int argc, char *argv[]) {

		Input input;
		get_inputs (argc, argv, input, 1);
		string 
		for (long i=0; i < MAT_DIM * MAT_DIM) {

		}
		// CITE: https://stackoverflow.com/questions/25201131/writing-csv-files-from-c
		ofstream input_file;
		input_file.open ("input_dat.csv");
		input_file << "This is the first cell in the first column.\n";
		input_file << "a,b,c,\n";
		input_file << "c,s,v,\n";
		input_file << "1,2,3.456\n";
		input_file << "semi;colon\n";
		input_file.close();


		cout << "\n\e[1;31mProgram End\e[0m\n\n\n";
		return 0;
}

