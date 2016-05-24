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
	print "Usage : perl yahoo_scrape.pl <csv|xls> [daily]\n";
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
	print "Usage : perl yahoo_scrape.pl <csv|xls> [daily]\n";
	exit;
}

#my $companies = get_company_data_files();
my @files = glob('CompanyData/*.csv');
my $companies = \@files;

my $daily = undef;

if(defined($ARGV[1])) {
	die "Invalid argument {$ARGV[1]}\n" unless $ARGV[1] eq "daily";
	my ($sec,$min,$hour,$mday,$mon,$year,$wday,$yday,$isdst) = localtime(); 
	my $y = 1900 + $year; 
	my $mon = $mon + 1;
	$daily = sprintf "$y-%0.2d-%0.2d", $mon, $mday;
}

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
		$final_data->{$symbol} = get_news($symbol, $daily);
	}

	write_file($final_data, $output_type, $ofname);


}

