package YConfig;

use Exporter;
use Readonly;

use DateTime;

@ISA = qw(Exporter);
@EXPORT_OK = qw(
	get_url
	get_output_type
	get_output_file_name
	get_company_data_files
);

Readonly $URL => 'https://au.finance.yahoo.com';
Readonly $OUTPUT => 'CSV';
Readonly $OUTPUT_FILE_NAME => 'yahoo_finanace_data';
Readonly @COMPANY_DATA => (
	#'CompanyData/AMEX.csv',
	#'CompanyData/NASDAQ.csv',
	#'CompanyData/NYSE.csv'
	'CompanyData/communications.csv'

);

sub get_url {
	return $URL;
}

sub get_output_type {
	return $OUTPUT;
}

sub get_output_file_name {
	my ( $file_type, $category ) = @_;

	my $file_name = $category . "-" . DateTime->now;

	if( uc($file_type) eq 'CSV' ) {
		$file_name .= '.csv';
	}
	elsif( uc($file_type) eq 'XLS' ) {
		$file_name .= '.xls';
	}

	return $file_name 
}

sub get_company_data_files {
	opendir(BIN, 'CompanyData') or die "Can't open $dir: $!";
	my @array = grep { -T "$dir/$_" } readdir BIN;
	return \@array;
}

1;
