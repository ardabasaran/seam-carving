package com.foreignOwl;

public class Main {
    public static void main(String[] args) throws Exception {
        SeamCarver carver = null;
        String output_file = null;
        String input_file = null;
        int output_width;
        int output_height;
        try {
            input_file = args[0];
            output_file = args[1];
            output_width = Integer.parseInt(args[2]);
            output_height = Integer.parseInt(args[3]);
        } catch(Exception e)
        {
            System.out.println("Usage: 'java SeamCarver [input file] [output file] [output width] [output height]");
            System.out.println(e);
            return;
        }

        carver = new SeamCarver(input_file);
        carver.saveEnergyTable();
        carver.carveImage(output_width, output_height);
        carver.saveCarvedImage(output_file, "jpg");
        carver.saveSeamTable();

    }
}
