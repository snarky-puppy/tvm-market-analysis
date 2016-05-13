package Scrape;

use URI;
use WWW::Mechanize;
use HTML::Entities;
use Exporter;
use Try::Tiny		qw(try catch);

use YConfig 		qw(get_url);

@ISA = 				qw( Exporter );
@EXPORT_OK = 		qw( get_news );

sub get_news {
	my ( $symbol, $i ) = @_;

	my @final_data = ();
	my $query_string = "/q/h?s=$symbol";
	while(1) {
		my $content = _get_content( $query_string );

	    my $data = _get_headlines_by_date( $content , $i);

	    last if(scalar @{$data->{data}} <= 0);

	    push @final_data, _prase_data($data->{data});

	    $query_string = $data->{nextpage}->{query_string};
	}

	return \@final_data
}

sub _generate_uri {
	my ($query_string) = @_;

	$query_string = '' unless $query_string;
	my $url = get_url() . $query_string;

	decode_entities($url);

	return URI->new( $url );
}

sub _get_content {
	my ( $query_string ) = @_;
	
	my $content = _request( $query_string );
	return _edit_content( $content )
}

sub _request {
	my ($query_string) = @_;

	my $mech = WWW::Mechanize->new();
	my $content;
	try {
    	$mech->get( _generate_uri($query_string) );
    	$content = $mech->content;
    }
    catch {
    	$content = '';
    };
    
    return $content;
}

sub _edit_content {
	my ($content) = @_;

	$content =~ s/<h3><span>/<div class="hello"><h3><span>/g;
    $content =~ s/<\/li><\/ul> <div class=\"hello\">/<\/li><\/ul><\/div><div class=\"hello\">/g;

    return $content;
}

sub _get_headlines_by_date {
	my ( $content ) = @_;

    my @divs = ( $content =~ /(<div class="hello">.+?<\/div>)/g);
    $content =~ /<a href="(\/q\/h\?.+)">Older Headlines/g;
    my $query_string = $1;

    my $data = {
        nextpage => {query_string => $query_string},
        data => \@divs || undef
    };
    print "URL : " . get_url() . $query_string . "\n";

    return $data;
}

sub _prase_data {
    my ($headlines) = @_;

    my $data = {};
    foreach my $headline ( @$headlines ) {
        $headline =~ /<h3><span>(.+?)<\/span><\/h3>/g;
        my $date = $1;
        my @titles = ( $headline =~ /<li><a.*?>(.+?)<\/a><cite>/g);

        $data->{$count++}->{$date} = [@titles];
    }
    return $data;
}

1;