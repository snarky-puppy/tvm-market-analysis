#!/usr/bin/perl
use strict;

use File::Basename;
use lib qw(./lib);

use Scrape 		qw(get_news);
use CSV 		qw(get_symbols);
use Write		qw(write_file);
use YConfig		qw(get_company_data_files);

if( scalar @ARGV > 3 ) {
	print "Error : Invalid number of arguments\n";
	print "Usage : perl yahoo_scrape.pl <csv|xls> [start, end]\n";
	exit;
}

my $output_type = '';
if( uc($ARGV[0]) eq 'XLS') {
	$output_type = 'XLS';
}
elsif( uc($ARGV[0]) eq 'CSV' ) {
	$output_type = 'CSV';
}
else {
	print "Error : Incorrect output type\n";
	print "Usage : perl yahoo_scrape.pl <csv|xls> [start, end]\n";
	exit;
}

#my $start = $ARGV[1] - 1;
#if( !$start || $start < 0 || $start > $symbols_length ) {
#	$start = 0;
#}
#
#my $end = $ARGV[2];
#$end =~ s/[^0-9]//g;
#
#if( !$end || $end == 0 || $end > $symbols_length ) {
#	$end = $symbols_length;
#}

#my $companies = get_company_data_files();
my @files = glob('CompanyData/*.csv');
my $companies = \@files;

my $count = 0;
foreach my $company ( @$companies ) {
	my $symbols = get_symbols($company);
	my @company_symbols;
	push @company_symbols, keys %{$symbols};

	my $symbols_length = ( scalar @company_symbols - 1 );

	@company_symbols = sort {uc($a) cmp uc($b)} @company_symbols;

	my $final_data = {};

	my @symbols_to_scrape = @company_symbols; #[$start..$end-1];

	my $ofname = basename($company);
	$ofname =~ s/.csv//;

	#print "$company - $ofname - $#symbols_to_scrape\n";

	foreach my $symbol ( @symbols_to_scrape ) {
	print "sym=$symbol\n";
		$final_data->{$symbol} = get_news($symbol);
	}

	write_file($final_data, $output_type, $ofname);


}

