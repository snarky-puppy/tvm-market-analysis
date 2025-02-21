package CSV;

use Exporter;
use DateTime::Format::Strptime;
use HTML::Entities;
use utf8;

@ISA = qw( Exporter );
@EXPORT_OK = qw(
	read_csv
	get_symbols
	create_csv
);

sub read_csv {
	my ($file) = @_;

	open FILE, '<', $file;
	my @csv_data;
	while(<FILE>) {
		push @csv_data, $_;
	}
	close FILE;

	return \@csv_data;
}

sub get_symbols {
	my ( $file ) = @_;

	my $csv_data = read_csv($file);

	my $symbols = {};
	my $header = 1;
	foreach my $line (@$csv_data) {
		if( $header == 1 && $line !~ /Symbol/) {
			$header = 0;
		} elsif( $header == 1 ) {
			$header = 0;
			next;
		}

		my @columns = split ',', $line;
		my $symbol = $columns[0];

		$symbol = remove_special_char( $symbol );

		$symbols->{$symbol} = 1;
	}
	return $symbols;
}

sub remove_special_char {
	my ( $symbol ) = @_;

	$symbol =~ s/\"//g;
	$symbol =~ s/\^[\w+]$//g;
	$symbol =~ s/\^$//g;
	#$symbol =~ s/\..+//g;

	$symbol =~ s/\s+$//g;
	$symbol =~ s/^\s+//g;

	return $symbol;
}

sub create_csv {
    my ( $data, $file_name ) = @_;

    open FILE, '>', $file_name;
    binmode(FILE, ":utf8");

    my $row = 1;
    foreach my $symbol ( keys %{$data} ) {
        foreach my $page ( @{$data->{$symbol}} ) {
            foreach my $count ( sort {$a <=> $b} keys %{$page} ) {
                foreach my $date ( keys %{$page->{$count}}) {
                    my $dt = _parse_date($date);
                    my $trailing_str = $dt->day_abbr() . " ". $dt->month() . " " . $dt->month_abbr;
                    my $line = '';
                    foreach my $headline( @{$page->{$count}->{$date}} ) {
                        $headline = qq{$headline ($trailing_str)};
                        utf8::decode($headline);
                        decode_entities($headline);

                        my $dmy = $dt->dmy;
                        $line = qq{"$dmy","$symbol","$headline"\n};
                        print FILE $line;
                    }
                }
            }
        }
    }
    close FILE;
}

sub _parse_date {
    my ( $date ) = @_;

    my $parser = DateTime::Format::Strptime->new(
      pattern => '%d %B %Y',
      on_error => 'croak',
    );

    return $parser->parse_datetime($date);
}

1;
