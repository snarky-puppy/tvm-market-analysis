package Excel;

use Exporter;
use Spreadsheet::WriteExcel;
use DateTime::Format::Strptime;
use HTML::Entities;
use utf8;

@ISA = qw( Exporter );
@EXPORT_OK = qw(
    create_excel
);

sub create_excel {
    my ( $data, $file_name ) = @_;

    my $workbook = Spreadsheet::WriteExcel->new($file_name);
    my $worksheet = $workbook->add_worksheet();

    my $format = $workbook->add_format();
    $format->set_bold();

    $worksheet->add_write_handler(qr[\w], \&_store_string_widths);

    $worksheet->write(0, 0, 'Date', $format);
    $worksheet->write(0, 1, 'Symbol', $format);
    $worksheet->write(0, 2, 'Headline', $format);

    my $row = 1;
    foreach my $symbol ( keys %{$data} ) {
        foreach my $page ( @{$data->{$symbol}} ) {
            foreach my $count ( sort {$a <=> $b} keys %{$page} ) {
                foreach my $date ( keys %{$page->{$count}}) {
                    my $dt = _parse_date($date);
                    my $trailing_str = $dt->day_abbr() . " ". $dt->month() . " " . $dt->month_abbr;
                    foreach my $headline( @{$page->{$count}->{$date}} ) {
                        $headline = qq{$headline ($trailing_str)};
                        utf8::decode($headline);
                        decode_entities($headline);

                        $worksheet->write($row, 0, $dt->dmy);
                        $worksheet->write($row, 1, $symbol);
                        $worksheet->write($row, 2, $headline);
                        $row++;
                    }
                }
            }
        }
    }
    _autofit_columns($worksheet);
    $workbook->close();
}

sub _autofit_columns {

    my $worksheet = shift;
    my $col       = 0;

    for my $width (@{$worksheet->{__col_widths}}) {

        $worksheet->set_column($col, $col, $width) if $width;
        $col++;
    }
}

sub _store_string_widths {

    my $worksheet = shift;
    my $col       = $_[1];
    my $token     = $_[2];

    return if not defined $token;
    return if $token eq '';
    return if ref $token eq 'ARRAY';
    return if $token =~ /^=/;

    # Ignore numbers
    return if $token =~ /^([+-]?)(?=\d|\.\d)\d*(\.\d*)?([Ee]([+-]?\d+))?$/;

    return if $token =~ m{^[fh]tt?ps?://};
    return if $token =~ m{^mailto:};
    return if $token =~ m{^(?:in|ex)ternal:};

    my $old_width    = $worksheet->{__col_widths}->[$col];
    my $string_width = _string_width($token);

    if (not defined $old_width or $string_width > $old_width) {
        $worksheet->{__col_widths}->[$col] = $string_width;
    }
    return undef;
}

sub _string_width {
    return 0.9 * length $_[0];
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