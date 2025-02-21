package Write;

use Exporter;

use CSV 			qw(create_csv);
use Excel 			qw(create_excel);
use YConfig 		qw(get_output_file_name);

@ISA = qw( Exporter );
@EXPORT_OK = qw(
	write_file
);

sub write_file {
	my ( $data, $file_type, $category ) = @_;

	my $file_name = get_output_file_name( $file_type, $category );

	print "file_name=$file_name\n";

	if($file_type && uc($file_type) eq 'XLS') {
		create_excel( $data, $file_name );
	}
	else {
		create_csv( $data, $file_name );
	}

}

1;
